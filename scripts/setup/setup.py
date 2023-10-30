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

def wrong(message):
    logger.error(message)
    sys.exit(1)

CURRENT_PATH = os.path.dirname(__file__)

# LOAD LOGGER
try:
    with open(CURRENT_PATH + '/config/logging.json', 'r') as logger:
        logging_config = json.load(logger)
    logging.config.dictConfig(logging_config)
except Exception as e:
    print("setup.py: ❌ Could not load logger configuation")
    print(e)
    sys.exit(1)

## Recover the name of the module for the main logger
logger = logging.getLogger(__name__)

# LOAD CONFIG
try: 
    with open(CURRENT_PATH + "/config/setup.json", "r") as setup:
        config = json.load(setup)

        eperson_groups = config["groups"]
        admin_login = config["admin_login"]
        users = config["users"]
        schemas = config["schemas"]
        collections = config["collections"]
        logger.debug("✅ Config loaded")
except Exception as e:
    logger.error("❌ An error occured while loading the configuration file ❌")
    logger.error(e)
    sys.exit(1)

# LOGIN AS ADMIN AND RETREIVE TOKEN

api = DspaceAPI("http://localhost:8080/server/api")
logger.info("⚙️ Connecting to admin account\r")
auth_res = api.signin(admin_login["email"], admin_login["pwd"])
if auth_res.ok: 
    auth_token = auth_res.headers["Authorization"]
    logger.info("✅ Connected to Admin account")
else:
    wrong("❌ Could not connect with the given admin email and password ❌")

# Accept user agreement for admin (Needed for some requests)
res = api.acceptUserAgreement(auth_token)
logger.info("✅ Accepted admin's users agreement")


# Once the user agreement has been accepted, we need to reconnect or refresh the token (that is what we do here)
auth_token = api.refreshConnectionWithToken(auth_token).headers["Authorization"]


#  Accept user agreement for other users
for user in users:
    res = api.acceptUserAgreement(auth_token, user["email"])
logger.info("✅ Accepted alt users agreement")


# ADD NEW METADATA SCHEMAS AND THEIR FIELDS
for schema in schemas:
    # Adding metadata schemas 
    res = api.addMetadataSchema(schema["prefix"], schema["namespace"], auth_token)
    if not res.ok:
        wrong("❌ An error occured while adding metadata schema: %s" % schema["prefix"])
    
    schema_res = res.json()
    schema_id = schema_res["id"]

    for field in schema["fields"]:
        # Adding metadata fields for the current schema
        res = api.addMetadataField(str(schema_id), auth_token, field["element"], field["qualifier"], field["scope_note"])
        if not res.ok:
            wrong("❌ An error occured while adding metadata field to %s" % str(schema_id))
logger.info("✅ Added new metadata schemas and fields")

# ADD NEW GROUPS AND THEN USERS TO THEM 
for group in eperson_groups:
    res = api.createEpersonGroup(auth_token, group["name"], group["description"])
    if not res.ok:
        wrong("❌ An error occured while creating group %s" % group["name"])

    group_info = res.json()
    group_id = group_info["id"]

    linked_users = filter(lambda x: x["group"] == group["name"], users)

    for user in linked_users:
        user_detail = api.getEpersonFromEmail(auth_token, user["email"]).json()
        user_id_path = user_detail["_links"]["self"]["href"]
        res = api.addEpersonToGroup(user_id_path, group_id, auth_token)
        if not res.ok:
            wrong("❌ An error occured while adding the user %s to the %s group" % (user["email"], group["name"]))
logger.info("✅ Added users groups")


# MANAGE COLLECTION RIGHTS
for collection in collections:
    collection_id = api.getCollectionFromName(collection["name"])["id"]
    
    for permission in collection["permissions"]:
        workflow_res = api.getWorkflowGroup(auth_token, collection_id, permission["type"])
        
        if workflow_res.status_code == 204:
            workflow_res = api.createWorkflowGroup(auth_token, collection_id, permission["type"])
            
            if workflow_res.status_code != 201:
                wrong("Error while creating the workflow group: " + permission["type"])
        
        workflow_group = workflow_res.json()
        
        for group in permission["groups"]:
            sub_group_uri = api.getGroupFromName(auth_token, group)["_links"]["self"]["href"]
            
            res = api.addSubGroupToGroup(workflow_group["id"], sub_group_uri, auth_token)
            if not res.ok:
                wrong("Error while linking child group %s to parent %s" % (group, workflow_group["id"]))
    logger.info("✅ Added %s collection rights" % collection["name"])

sys.exit(0)