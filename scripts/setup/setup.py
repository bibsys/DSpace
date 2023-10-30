from dspaceAPI import DspaceAPI
import json
import sys
import logging
import logging.config
import os 


#########################################
# This is a script to do some tasks that 
# are not possible via the Dspace CLI. 
# Here we use the "DspaceAPI" class that 
# implements all the HTTP calls we need.
#########################################

CURRENT_PATH = os.path.dirname(__file__)

def wrong(message=None, exception=None, exit_code=1):
    """Log an error message and/or the corresponding exception
    :param message: the error message to log.
    :param exception: the ``Exception`` to capture into log.
    :param exit_code: the exit code to return
    """
    assert message or exception, "At least one of the two parameters (message OR exception) is required"

    logger.error(message or str(exception))
    if exception:
        logger.exception(exception)
    sys.exit(exit_code)

# LOAD LOGGER CONFIG
try:
    with open(f'{CURRENT_PATH}/config/logging.json', 'r') as logger:
        logging_config = json.load(logger)
    logging.config.dictConfig(logging_config)
except Exception as e:
    print("❌ Could not load logger configuation")
    print(e)
    sys.exit(1)

## Recover the name of the script for the main logger
logger = logging.getLogger(__name__)

# LOAD CONFIG
try: 
    with open(f"{CURRENT_PATH}/config/users.json", "r") as u:
        config = json.load(u)
        users = config['users']
        admin_login = config['admin_login']
    with open(f"{CURRENT_PATH}/config/groups.json", "r") as g:
        eperson_groups = json.load(g)['groups']
    with open(f"{CURRENT_PATH}/config/schemas.json", "r") as s:
        schemas = json.load(s)['schemas']
    with open(f"{CURRENT_PATH}/config/collections.json", "r") as c:
        collections = json.load(c)['collections']
    logger.debug("✅ Config loaded")
except Exception as e:
    logger.error("❌ An error occured while loading the configuration files")
    logger.error(e)
    sys.exit(1)

# LOGIN AS ADMIN AND RETREIVE TOKEN
#   We will use the backend API to login as administrator and perform multiple items creation.
#   To have full access, we also need to validate the users agreements, after which we need to 
#   refresh the authentication token previsously generated when logging in to gain full management
#   possibilities. 
api = DspaceAPI("http://localhost:8080/server/api")
logger.info("⚙️ Connecting to admin account\r")
auth_res = api.login(admin_login['email'], admin_login['pwd'])
if auth_res.ok: 
    auth_token = auth_res.headers['Authorization']
    logger.info("✅ Connected to Admin account")
else:
    wrong("❌ Could not connect with the given admin email and password")

# Accept user agreement for admin (Needed for some requests)
res = api.acceptUserAgreement(auth_token)
logger.info("✅ Accepted admin's users agreement")

# Once the user agreement has been accepted, we need to reconnect or refresh the token (that is what we do here)
auth_token = api.refreshConnectionWithToken(auth_token).headers['Authorization']

#  Accept user agreement for other users
for user in users:
    res = api.acceptUserAgreement(auth_token, user['email'])
logger.info("✅ Accepted alt users agreement")

# ADD NEW METADATA SCHEMAS AND THEIR FIELDS
for schema in schemas:
    # Adding metadata schemas 
    res = api.addMetadataSchema(schema['prefix'], schema['namespace'], auth_token)
    if res.ok:
        logger.debug(f"✅ {schema['prefix']} schema added to dspace")
    else:
        wrong(f"❌ An error occured while adding metadata schema: {schema['prefix']}")
    
    schema_res = res.json()
    schema_id = schema_res['id']

    for field in schema['fields']:
        # Adding metadata fields for the current schema
        res = api.addMetadataField(str(schema_id), auth_token, field['element'], field['qualifier'], field['scope_note'])
        if res.ok:
            logger.debug(f"✅ {field['element']}.{field['qualifier']} field added to {schema['prefix']}")
        else:
            wrong(f"❌ An error occured while adding metadata field to {str(schema_id)}")
logger.info("✅ Added new metadata schemas and fields")

# ADD NEW GROUPS AND THEN USERS TO THEM 
#   Use the API to create the groups specified in the config file and then add users to them
for group in eperson_groups:
    res = api.createEpersonGroup(auth_token, group['name'], group['description'])
    if res.ok:
        logger.debug(f"✅ {group['name']} group added to dspace")
    else:
        wrong(f"❌ An error occured while creating group {group['name']}")

    group_info = res.json()
    group_id = group_info['id']
    linked_users = filter(lambda x: x['group'] == group['name'], users)

    for user in linked_users:
        user_detail = api.getEpersonFromEmail(auth_token, user['email']).json()
        user_id_path = user_detail['_links']['self']['href']
        res = api.addEpersonToGroup(user_id_path, group_id, auth_token)
        if res.ok:
            logger.debug(f"✅ {user['email']} added to {group['name']} group")
        else:
            wrong(f"❌ An error occured while adding the user {user['email']} to the {group['name']} group")
logger.info("✅ Added users groups")

# MANAGE COLLECTION RIGHTS
#   From the config file, add the correct rights to each users group for a given collection 
#   Each type of user has a precise role in the submission workflow which is specified in the config file
for collection in collections:
    collection_id = api.getCollectionFromName(collection['name'])['id']
    
    for permission in collection['permissions']:
        workflow_res = api.getWorkflowGroup(auth_token, collection_id, permission['type'])
        
        if workflow_res.status_code == 204:
            workflow_res = api.createWorkflowGroup(auth_token, collection_id, permission['type'])
            
            if workflow_res.status_code != 201:
                wrong("❌ Error while creating the workflow group: " + permission['type'])
        
        workflow_group = workflow_res.json()
        
        for group in permission['groups']:
            sub_group_uri = api.getGroupFromName(auth_token, group)['_links']['self']['href']
            
            res = api.addSubGroupToGroup(workflow_group['id'], sub_group_uri, auth_token)
            if res.ok:
                logger.debug(f"✅ Successfully granted permission to {group} group")
            else:
                wrong(f"❌ Error while linking child group {group} to parent {workflow_group['id']}")
    logger.info(f"✅ Added {collection['name']} collection rights")
