import requests
import urllib.parse
import json


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
    def __init__(self, url):
        self.url = url
        self.httpSession = requests.Session()
        self.cookies = self.httpSession.cookies

        self.token = ""

        self.user_email = ""
        self.uuid = ""

        self.worklow_types = {
            "submit": "/submittersGroup", 
            "reviewer": "/workflowGroups/reviewer", 
            "editor": "/workflowGroups/editor", 
            "finaleditor": "/workflowGroups/finaleditor", 
            "bitstreamread": "/bitstreamReadGroup",
            "itemread": "/itemReadGroup"
        }
    
    def __generateAuthHeaders(self, auth_token): 
        """
        PRIVATE METHOD: __generateAuthHeaders(auth_token)
        -------------------------------------------------
        Generate a header dictionnary that can be used to make request while beeing authenticated.
        
        :return:(Response Object) The response of the request made on the base API url. 
        """
        return {
            "Accept": "*/*",
            "X-XSRF-TOKEN": self.getXSRF(),
            "Authorization": auth_token
        }

    def getRoot(self):
        """
        getRoot(self)
        ----------------------------
        Call the base url of the backend api. In most cases used to retrieve the XSRF token.
        
        :return:(Response Object) The response of the request made on the base API url. 
        """
        res = self.httpSession.get(self.url)
        if res.status_code != 200:
            raise Exception("Error while calling the api url, got status code " + res.status_code + " !")
        return res

    def getXSRF(self): 
        """
        getXSRF()
        ----------------------------
        Retreives the XSRF Token. Refreshes the cookies on each request.
        
        :return:(str) The XSRF Token
        """
        self.getRoot()
        return self.cookies.get("DSPACE-XSRF-COOKIE")
    
    ##############
    # Auth methods 
    ##############

    def signin(self, email, password):
        """
        signin(email, password)
        --------------------------------------------
        This method simulate a connection from a user using an email and a password.

        :param email:(str) The mail of the account to connect to
        :param password:(str) The password of the account to connect to
        :return:(Response Object): The response from the query to the API ("/authn/login")
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
        if res.status_code == 200:
            self.user_email = email
        return res
    
    def refreshConnectionWithToken(self, auth_token):
        headers = self.__generateAuthHeaders(auth_token)

        res = self.httpSession.post(self.url + "/authn/login", headers=headers)
        return res

    def getAuthStatus(self, auth_token):
        """
        getAuthStatus(auth_token)
        ----------------------------------------
        Retreive information about the current openned user session such as auth method or user url.
        
        :param auth_token:(str) The token generated when connecting with an admin account.
        :return:(Response Object) The response of the request.
        """
        headers = self.__generateAuthHeaders(auth_token)

        res = self.httpSession.get(self.url + "/authn/status", headers=headers)
        return res
    
    
    ################
    # EPERSON METHOD
    ################

    def getAllEpersons(self, auth_token):
        """
        getAllEpersons(auth_token)
        -----------------------------------------
        Retreive a list of all the current registered users.

        :param auth_token:(str) The token generated when connecting with an admin account.
        :return:(Response Object) The response of the request.
        """
        headers = self.__generateAuthHeaders(auth_token)

        res = self.httpSession.get(self.url + "/eperson/epersons/", headers=headers)
        return res
    
    def getEpersonFromEmail(self, auth_token, email=None):
        """
        getEpersonFromEmail(auth_token, email=None)
        ----------------------------------------------------------
        Search user's informations based on his email address.
        If no email provided (None), it will recover the current logged in user's informations.

        :param auth_token:(str) The token generated when connecting with an admin account.
        :param email: (OPTIONAL string) The email of the user you want to retreive information of.
        :return:(Response Object) The response of the request.
        """
        target_email = email

        # If no email provided, we recover the current user's email        
        if email is None:
            if not self.user_email:
                raise Exception("Error: Authenticate a user before retreiving current user informations")
            target_email = self.user_email
        
        headers= self.__generateAuthHeaders(auth_token)

        res = self.httpSession.get(self.url + "/eperson/epersons/search/byEmail?email=" + urllib.parse.quote(target_email), headers=headers)
        return res
    
    # ADDING PWD NOT WORKING => USE CLI INSTEAD TO ADD USERS WITH PASSWORD
    # PROBLEM: An added user must create his password by himself (trough a link sended by mail)
    # def createEperson(self, email, firstname, lastname, password, auth_token):
    #     headers = {
    #         "Accept": "*/*",
    #         "X-XSRF-TOKEN": self.getXSRF(),
    #         "Authorization": auth_token
    #     }
    #     user_object = {
    #         "name": email,
    #         "metadata": {
    #             "eperson.firstname": [{"value": firstname}],
    #             "eperson.lastname": [{"value": lastname}]
    #         },
    #         "canLogIn": True,
    #         "email": email,
    #         "requireCertificate": False,
    #         "selfRegistered": True,
    #         "type": "eperson"
    #     } 
    #     res = self.httpSession.post(self.url + "/eperson/epersons", json=user_object, headers=headers)
    #     if res.status_code != 201:
    #         raise Exception("Could not create the user")
    #     operation = {
    #         "op": "add",
    #         "path": "/password",
    #         "value": [
    #             {"new_password": password, "current_password": ""},
    #         ]
    #     }
    #     res_pwd = self.patchEpersonInfo(res.json()["id"], [operation], auth_token)
    #     print(res_pwd.text)
    #     return res

    def patchEpersonInfo(self, eperson_id, operation, auth_token):
        """
        patchEpersonInfo(eperson_id, operation, auth_token)
        ------------------------------------------------------------------
        Can be used to modify user's metadata.

        :param eperson_id:(str) UUID of the target user
        :param operation:(dict) A dictionnary describing the operation to apply for the selected user
        :return:(Response Object) The response of the request made on the API url. 
        """
        headers = self.__generateAuthHeaders(auth_token)

        res = self.httpSession.patch(self.url + "/eperson/epersons/" + eperson_id, json=operation, headers=headers)
        return res

    def acceptUserAgreement(self, auth_token, email=None):
        """
        acceptUserAgreement(auth_token)
        ----------------------------------------------
        Accept the user agreements for a specific user. 
        Usefull for developement environement.

        :param auth_token:(str) The token generated when connecting with an admin account.
        :return: None
        """

        # Retreive the current logged in user's address  
        info = self.getEpersonFromEmail(auth_token, email)

        if info.status_code != 200:
            raise Exception("Error while retreiving current user information (" + self.user_email + ")!")
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
                        {"value": "true"},
                    ]
                }
                res = self.patchEpersonInfo(uuid, [operation], auth_token)
                if res.status_code != 200:
                    raise Exception("Error while patching user informations")
        except Exception as e:
            raise Exception("Error while extracting the user metadata:\n" + str(e))
        return 0
    
    #######################
    # EPERSON GROUP METHODS 
    #######################

    def getAllGroups(self, auth_token):
        return self.httpSession.get(self.url + "/eperson/groups", headers=self.__generateAuthHeaders(auth_token))

    def getGroupFromName(self, auth_token, group_name):
        """
        getGroupFromName(auth_token, name)
        ------------------------------------------
        Return the corresponding group object based on the name

        :param auth_token: The token generated when connecting with an admin account.
        :param group:(str) The name of the group that will be retrieved
        """
        headers = self.__generateAuthHeaders(auth_token)

        groups_list = self.getAllGroups(auth_token).json()["_embedded"]["groups"]

        target_group = next(filter(lambda x: x["name"] == group_name, groups_list))
        return target_group

    def createEpersonGroup(self, auth_token, name, description):
        """
        createEpersonGroup(auth_token, name, description)
        ---------------------------------------------------------------
        Used to create a new user group by providing its name and description.
        Once created, it can be linked to users with the addEpersonToGroup() method.
        
        :param auth_token:(str) The token generated when connecting with an admin account.
        :param name:(str) The name of the group to create.
        :param description:(str) The description of the group to create.
        :return:(Response Object) The response of the request made on the API url. 
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

        res = self.httpSession.post(self.url + "/eperson/groups", json=obj, headers=headers)
        return res

    def addEpersonToGroup(self, erperson_uri, group_id, auth_token):
        """
        createEpersonGroup(erperson_uri, group_id, auth_token)
        ------------------------------------------------------------
        After creating a group with createEpersonGroup(), you can add user to it using this method.

        :param erperson_uri:(str) The uri representing the path to the user.
        :param group_id:(str) The id of the group you want to add user in.
        :param auth_token:(str) The token generated when connecting with an admin account.
        :return:(Response Object) The response of the request made on the API url.
        """
        headers = self.__generateAuthHeaders(auth_token)        
        headers["Content-Type"] = "text/uri-list"

        res = self.httpSession.post(self.url + "/eperson/groups/" + group_id + "/epersons", data=erperson_uri, headers=headers)
        return res
    
    def addSubGroupToGroup(self, group_id, subgroup_uri, auth_token):
        """
        addSubGroupToGroup(group_id, subgroup_uri, auth_token)
        ------------------------------------------------------
        Add a group to another group, usefull for granting rights for collections and communities to users groups

        :param group_id:(str) Id of the praent group
        :param subgroup_uri:(str) Id of the child group 
        :param auth_token:(str) The token generated when connecting with an admin account.
        :return:(Response Object) The response of the request made on the API url.
        """
        headers = self.__generateAuthHeaders(auth_token)
        headers["Content-Type"] = "text/uri-list"

        res = self.httpSession.post(self.url + "/eperson/groups/" + group_id + "/subgroups", data=subgroup_uri, headers=headers)
        return res
    

    ##################
    # METADATA METHODS
    ##################

    def getMetadataSchemas(self):
        """
        getMetadataSchemas()
        -----------------------------------
        Retreive all the metadata schema from the API.

        :return:(Response Object) The response of the request made on the API url. 
        """
        headers = {
            "Accept": "*/*",
            "X-XSRF-TOKEN": self.getXSRF(),
        }
        
        res = self.httpSession.get(self.url + "/core/metadataschemas", headers=headers)
        return res
    
    def addMetadataSchema(self, prefix, namespace, auth_token):
        """
        addMetadataSchema(prefix, namespace, auth_token)
        ---------------------------------------------------------------
        Add a new metadata schema to the database. 

        :param prefix:(str) The prefix of the new metadata schema.
        :param namespace:(str) The namespace definition of the new metadata schema.
        :param auth_token:(str) The token generated when connecting with an admin account.
        :return:(Response Object) The response of the request made on the API url. 
        """
        headers = self.__generateAuthHeaders(auth_token)        
        
        json = {
            "prefix": prefix, 
            "namespace": namespace
        }

        res = self.httpSession.post(self.url + "/core/metadataschemas", json=json, headers=headers)
        return res
    
    def addMetadataField(self, schema_id, auth_token, element, qualifier="", scope_note=""):
        """
        addMetadataField(schema_id, auth_token, element, qualifier="", scope_note="")
        --------------------------------------------------------------------------------------------
        Add a new field to an existing metadata schema. It needs to contain at least an element.

        :param schema_id:(str) The id of the schema which the field will be link to.
        :param auth_token:(str) The token generated when connecting with an admin account.
        :param element:(REQUIRED str) The element part of the field.
        :param qualifier:(str) The qualifier part of the field.
        :param scope_note:(str) The scope note part of teh field. 
        :return:(Response Object) The response of the request made on the API url. 
        """
        headers = self.__generateAuthHeaders(auth_token)

        json = {
            "element": element,
            "qualifier": qualifier,
            "scopeNote": scope_note
        }

        res = self.httpSession.post(self.url + "/core/metadatafields?schemaId=" + schema_id, json=json, headers=headers)
        return res
    
    ###################
    # ResourcePolicies
    ###################

    def createResourcePolicy(self, targetUUID, auth_token, resourcePolicyDetails, epersonUUID=None, groupUUID=None):
        """
        Create a resource policy in the Dspace App and grant it to a group or a user.
        """
        if (epersonUUID and groupUUID):
            raise Exception("epersonUUID and groupUUID cannot be both set !")
        if not (epersonUUID or groupUUID):
            raise Exception("You must indicate either the epersonUUID or the groupUUID !")

        headers = self.__generateAuthHeaders(auth_token)

        route = "/authz/resourcepolicies?resource=" + targetUUID + "&"
        
        additional = "eperson=" + epersonUUID if epersonUUID else "group=" + groupUUID

        res = self.httpSession.post(self.url + route + additional, headers=headers, json=resourcePolicyDetails)
        return res
    
    #############
    # Collections
    #############

    def getAllCollections(self):
        """
        getAllCollections()
        -------------------
        Get all collections presents in the dspace API.
        """
        res = self.httpSession.get(self.url + "/core/collections", headers={"Accept": "*/*", "X-XSRF-TOKEN": self.getXSRF()})
        return res
    
    def getWorkflowGroup(self, auth_token, collection_id, type):
        """
        getWorkflowGroup(auth_token, collection_id, type)
        -------------------------------------------------
        Get a list of all existing workflow groups for a given collection.
        """
        types = self.worklow_types

        if type not in types.keys():
            raise Exception("Invalid type")
        
        headers = self.__generateAuthHeaders(auth_token)

        res = self.httpSession.get(self.url + "/core/collections/" + collection_id + types[type], headers=headers)
        return res

    def createWorkflowGroup(self, auth_token, collection_id, type):
        """
        createWorkflowGroup(auth_token, collection_id, type)
        ----------------------------------------------------
        Create a workflow group for a given colletion. 
        The type of the workflow must be one of the dict "workflow_types" created in the __init__() of the instance.
        """
        types = self.worklow_types

        if type not in types.keys():
            raise Exception("Invalid type")
        
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

        res = self.httpSession.post(self.url + "/core/collections/" + collection_id + types[type], json=data, headers=headers)
        return res
    
    def getCollectionFromName(self, collection_name):
        """
        getCollectionFromName(name)
        ------------------------------------------
        Return the corresponding collection object based on the name

        :param collection_name:(str) The name of the collection that will be retrieved
        """
        collection_list = self.getAllCollections().json()["_embedded"]["collections"]
        collection = next(filter(lambda x: x["name"] == collection_name, collection_list))
        
        return collection