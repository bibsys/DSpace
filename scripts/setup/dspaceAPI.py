from requests import Session, Response, codes, HTTPError
import urllib.parse

# Exceptions
class AuthenticationError(Exception):
    def __init__(self, message) -> None:
        self.message = message
        super().__init__(self.message)

class InvalidWorkflowType(Exception):
    def __init__(self, message="Invalid workflow type"):
        self.worklow_types = {
            'submit': '/submittersGroup', 
            'reviewer': '/workflowGroups/reviewer', 
            'editor': '/workflowGroups/editor', 
            'finaleditor': '/workflowGroups/finaleditor', 
            'bitstreamread': '/bitstreamReadGroup',
            'itemread': '/itemReadGroup'
        }
        self.message = message
        types_message = f'Type must be one of the following: {", ".join(self.worklow_types.keys())}'
        super().__init__(f'{self.message}\n{types_message}')

class BadParameterError(Exception):
    def __init__(self, message="Bad method parameter"):
        self.message = message
        super().__init__(self.message)

# Main Class
class DspaceAPI():
    """
    Main class to manage Dspace API calls.
    The goal is to handle the majority of auth steps and calls to API in this class.
    
    The Dspace 7 API doc can be found here: https://github.com/DSpace/RestContract/tree/main

    Attributes:
    -----------
    url: str
        The main API url provided when instanciating
    httpSession: Session
        The session used by all the methods to make request and harvest cookies
    token: str
        The XSRF token used in all API calls 
    user_email: str
        The mail of the current logged in user
    uuid: str 
        The uuid of the current logged in user
    """
    def __init__(self, url: str) -> None:
        """
        Initialize the DspaceAPI class

        :param url: The main url to access the Dspace API
        """
        self.url = url
        self.httpSession = Session()
        self.cookies = self.httpSession.cookies

        self.token = ""

        self.user_email = ""
        self.uuid = ""

        self.worklow_types = {
            'submit': '/submittersGroup', 
            'reviewer': '/workflowGroups/reviewer', 
            'editor': '/workflowGroups/editor', 
            'finaleditor': '/workflowGroups/finaleditor', 
            'bitstreamread': '/bitstreamReadGroup',
            'itemread': '/itemReadGroup'
        }
    
    def __generateAuthHeaders(self, auth_token: str) -> dict: 
        """
        Generate a header dictionnary that can be used to make request while beeing authenticated.
        
        :return: The response of the request made on the base API url. 
        """
        return {
            "Accept": "*/*",
            "X-XSRF-TOKEN": self.getXSRF(),
            "Authorization": auth_token
        }

    def getRoot(self) -> Response:
        """
        Call the base url of the backend api. In most cases used to retrieve the XSRF token.
        
        :return: The response of the request made on the base API url. 
        """
        res = self.httpSession.get(self.url)
        if res.status_code != 200:
            raise HTTPError(f"Error while calling the api url, got status code {res.status_code} !")
        return res

    def getXSRF(self) -> str: 
        """
        Retrieves the XSRF Token. Refreshes the cookies on each request.
        
        :return: The XSRF Token
        """
        self.getRoot()
        return self.cookies.get("DSPACE-XSRF-COOKIE")
    
    ##############
    # Auth methods 
    ##############

    def login(self, email: str, password: str) -> Response:
        """
        This method simulate a connection from a user using an email and a password.

        :param email: The mail of the account to connect to
        :param password: The password of the account to connect to
        :return: The response from the query to the API ("/authn/login")
        """
        data = {
            "user": urllib.parse.quote(email),
            "password": urllib.parse.quote(password)
        }
        headers = {
            "Accept": "*/*",
            "X-XSRF-TOKEN": self.getXSRF(),
        }
        route = f"/authn/login?user={data['user']}&password={data['password']}"
        res = self.httpSession.post(self.url + route, data=data, headers=headers, cookies=self.cookies)
        if res.status_code == codes.ok:
            self.user_email = email
        else:
            raise AuthenticationError("Could not login as {email}!")
        return res
    
    def refreshConnectionWithToken(self, auth_token: str) -> Response:
        """
        Refreshes the auth_token with a new one.

        :return: The response from the login route
        """
        headers = self.__generateAuthHeaders(auth_token)
        return self.httpSession.post(f"{self.url}/authn/login", headers=headers)

    def getAuthStatus(self, auth_token: str) -> Response:
        """
        Retrieve information about the current openned user session such as auth method or user url.
        
        :param auth_token: The token generated when connecting with an admin account.
        :return: The response of the request.
        """
        headers = self.__generateAuthHeaders(auth_token)
        return self.httpSession.get(f"{self.url}/authn/status", headers=headers)
    
    
    ################
    # EPERSON METHOD
    ################

    def getAllEpersons(self, auth_token: str) -> Response:
        """
        Retrieve a list of all the current registered users.

        :param auth_token: The token generated when connecting with an admin account.
        :return: The response of the request.
        """
        headers = self.__generateAuthHeaders(auth_token)
        return self.httpSession.get(f"{self.url}/eperson/epersons/", headers=headers)
    
    def getEpersonFromEmail(self, auth_token: str, email: str | None = None) -> Response:
        """
        Search user's informations based on his email address. If no email provided (None), it will recover the current logged in user's informations.

        :param auth_token: The token generated when connecting with an admin account.
        :param email: The email of the user you want to retrieve information of.
        :return: The response of the request.
        """
        target_email = email or self.user_email

        # If no email provided, we recover the current user's email        
        if not target_email:
            raise AuthenticationError("Authenticate a user before retrieving current user informations")
        
        headers= self.__generateAuthHeaders(auth_token)
        return self.httpSession.get(f"{self.url}/eperson/epersons/search/byEmail?email={urllib.parse.quote(target_email)}", headers=headers)

    def patchEpersonInfo(self, eperson_id: str, operation: dict, auth_token: str) -> Response:
        """
        Can be used to modify user's metadata.

        :param eperson_id: UUID of the target user
        :param operation: A dictionnary describing the operation to apply for the selected user
        :return: The response of the request made on the API url. 
        """
        headers = self.__generateAuthHeaders(auth_token)
        return self.httpSession.patch(f"{self.url}/eperson/epersons/{eperson_id}", json=operation, headers=headers)

    def acceptUserAgreement(self, auth_token: str, email: str | None = None):
        """
        Accept the user agreements for a specific user, which is usefull for developement environement.

        :param auth_token: The token generated when connecting with an admin account.
        :return: None
        """

        # Retrieve the current logged in user's address  
        info = self.getEpersonFromEmail(auth_token, email)

        if info.status_code != 200:
            raise AuthenticationError(f"Error while retrieving current user information ({self.user_email })!")
        try:
            # Check if the users agreement have been accepted before
            # IF NOT: Accept them with the self.patchEpersonInfo() method
            metadata = info.json()["metadata"]
            if "dspace.agreements.end-user" not in metadata.keys():
                uuid = info.json()["uuid"]
                operation = {
                    "op": "add",
                    "path": "/metadata/dspace.agreements.end-user",
                    "value": [
                        {"value": True},
                    ]
                }
                res = self.patchEpersonInfo(uuid, [operation], auth_token)
                if res.status_code != 200:
                    raise HTTPError("Error while patching user informations")
        except Exception as e:
            raise HTTPError(f"Error while extracting the user metadata:\n{str(e)}") from e 
    
    #######################
    # EPERSON GROUP METHODS 
    #######################

    def getAllGroups(self, auth_token: str) -> Response:
        return self.httpSession.get(f"{self.url}/eperson/groups", headers=self.__generateAuthHeaders(auth_token))

    def getGroupFromName(self, auth_token: str, group_name: str) -> dict:
        """
        Return the corresponding group object based on the name

        :param auth_token: The token generated when connecting with an admin account.
        :param group: The name of the group that will be retrieved
        :return: If found, a dict containing the element informations, if not, an empty dict 
        """
        headers = self.__generateAuthHeaders(auth_token)

        groups_list = self.getAllGroups(auth_token).json()["_embedded"]["groups"]
        return next(filter(lambda x: x["name"] == group_name, groups_list))

    def createEpersonGroup(self, auth_token: str, name: str, description: str) -> Response:
        """
        Used to create a new user group by providing its name and description.
        Once created, it can be linked to users with the addEpersonToGroup() method.
        
        :param auth_token: The token generated when connecting with an admin account.
        :param name: The name of the group to create.
        :param description: The description of the group to create.
        :return: The response of the request made on the API url. 
        """
        headers = self.__generateAuthHeaders(auth_token)

        obj = {
            "name": name,
            "metadata": {
                "dc.description":[
                    {
                        "value": description,
                    }
                ]
            }
        }

        return self.httpSession.post(f"{self.url}/eperson/groups", json=obj, headers=headers)

    def addEpersonToGroup(self, erperson_uri: str, group_id: str, auth_token: str) -> Response:
        # sourcery skip: class-extract-method
        """
        After creating a group with createEpersonGroup(), you can add user to it using this method.

        :param erperson_uri: The uri representing the path to the user.
        :param group_id: The id of the group you want to add user in.
        :param auth_token: The token generated when connecting with an admin account.
        :return: The response of the request made on the API url.
        """
        headers = self.__generateAuthHeaders(auth_token)        
        headers["Content-Type"] = "text/uri-list"
        return self.httpSession.post(f"{self.url}/eperson/groups/{group_id}/epersons", data=erperson_uri, headers=headers)
    
    def addSubGroupToGroup(self, group_id: str, subgroup_uri: str, auth_token: str) -> Response:
        """
        Add a group to another group, usefull for granting rights for collections and communities to users groups

        :param group_id: Id of the praent group
        :param subgroup_uri: Id of the child group 
        :param auth_token: The token generated when connecting with an admin account.
        :return: The response of the request made on the API url.
        """
        headers = self.__generateAuthHeaders(auth_token)
        headers["Content-Type"] = "text/uri-list"

        return self.httpSession.post(f"{self.url}/eperson/groups/{group_id}/subgroups", data=subgroup_uri, headers=headers)
    
    ##################
    # METADATA METHODS
    ##################

    def getMetadataSchemas(self) -> Response:
        """
        Retrieve all the metadata schema from the API.

        :return: The response of the request made on the API url. 
        """
        headers = {
            "Accept": "*/*",
            "X-XSRF-TOKEN": self.getXSRF(),
        }
        
        return self.httpSession.get(f"{self.url}/core/metadataschemas", headers=headers)
    
    def addMetadataSchema(self, prefix: str, namespace: str, auth_token: str) -> Response:
        """
        Add a new metadata schema to the database. 

        :param prefix: The prefix of the new metadata schema.
        :param namespace: The namespace definition of the new metadata schema.
        :param auth_token: The token generated when connecting with an admin account.
        :return: The response of the request made on the API url. 
        """
        headers = self.__generateAuthHeaders(auth_token)        
        
        json = {
            "prefix": prefix, 
            "namespace": namespace
        }

        return self.httpSession.post(f"{self.url}/core/metadataschemas", json=json, headers=headers)
    
    def addMetadataField(self, schema_id: str, auth_token: str, element: str, qualifier:str = "", scope_note:str = "") -> Response:
        """
        Add a new field to an existing metadata schema. It needs to contain at least an element.

        :param schema_id: The id of the schema which the field will be link to.
        :param auth_token: The token generated when connecting with an admin account.
        :param element: The element part of the field.
        :param qualifier: The qualifier part of the field.
        :param scope_note: The scope note part of teh field. 
        :return: The response of the request made on the API url. 
        """
        headers = self.__generateAuthHeaders(auth_token)

        json = {
            "element": element,
            "qualifier": qualifier,
            "scopeNote": scope_note
        }

        return self.httpSession.post(f"{self.url}/core/metadatafields?schemaId={schema_id}", json=json, headers=headers)
    
    ###################
    # ResourcePolicies
    ###################

    def createResourcePolicy(self, targetUUID: str, auth_token: str, resourcePolicyDetails: str, epersonUUID: str | None = None, groupUUID: str | None = None)  -> Response:
        """
        Create a resource policy in the Dspace App and grant it to a group or a user.

        :param targetUUID: The UUID of the ressource you want to add rigths to.
        :param auth_token: The token generated when connecting with an admin account.
        :param resourcePolicyDetails: Details for the rights that will be granted.
        :param epersonUUID:The UUID of the user which you want to add permissions to. 
        :param groupUUID: The UUID of the group which you want to add permissions to.
        :return: The response of the request made on the API url.
        """
        if epersonUUID and groupUUID:
            raise BadParameterError("Parameters epersonUUID and groupUUID cannot be both set !")
        if not (epersonUUID or groupUUID):
            raise BadParameterError("You must indicate either the epersonUUID or the groupUUID parameter !")

        headers = self.__generateAuthHeaders(auth_token)

        route = f"/authz/resourcepolicies?resource={targetUUID}&"
        
        additional = f"eperson={epersonUUID or f'group={groupUUID}'}"

        return self.httpSession.post(self.url + route + additional, headers=headers, json=resourcePolicyDetails)
    
    #############
    # Collections
    #############

    def getAllCollections(self) -> Response:
        """
        Get all collections presents in the dspace API.

        :return: The response of the request made on the API url.
        """
        return self.httpSession.get(f"{self.url}/core/collections", headers={"Accept": "*/*", "X-XSRF-TOKEN": self.getXSRF()})
    
    def getWorkflowGroup(self, auth_token: str, collection_id: str, type: str) -> Response:
        """
        Get a list of all existing workflow groups for a given collection.

        :param auth_token: The token generated when connecting with an admin account.
        :param collection_id: The target collection id.
        :param type: The type of right information you want to retrieve, must be in keys of "self.workflow_types".
        :return: The response of the request made on the API url.
        """
        types = self.worklow_types

        if type not in types.keys():
            raise InvalidWorkflowType("Invalid workflow type")
        
        headers = self.__generateAuthHeaders(auth_token)

        return self.httpSession.get(f"{self.url}/core/collections/{collection_id}{types[type]}", headers=headers)

    def createWorkflowGroup(self, auth_token: str, collection_id: str, type: str) -> Response:
        """
        Create a workflow group for a given colletion. The type of the workflow must be one of the dict "workflow_types" created in the __init__() of the instance.
        
        :param auth_token: The token generated when connecting with an admin account.
        :param collection_id: The target collection id.
        :param type: The type of right information you want to add, must be in keys of "self.workflow_types".
        :return: The response of the request made on the API url.
        """
        types = self.worklow_types

        if type not in types.keys():
            raise InvalidWorkflowType("Invalid workflow type")
        
        headers = self.__generateAuthHeaders(auth_token)

        data = {
            "metadata": {
                "dc.description": [
                    {
                        "value": type
                    }
                ]
            }
        }

        return self.httpSession.post(f"{self.url}/core/collections/{collection_id}{types[type]}", json=data, headers=headers)
    
    def getCollectionFromName(self, collection_name: str) -> dict:
        """
        Return the corresponding collection object based on the name

        :param collection_name: The name of the collection that will be retrieved
        :return: If found, a dict containing the element informations, if not, an empty dict 
        """
        collection_list = self.getAllCollections().json()["_embedded"]["collections"]        
        return next(filter(lambda x: x["name"] == collection_name, collection_list))