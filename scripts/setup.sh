#!/bin/bash

# =============================================================================
#     CUSTOM FUNCTIONS
# =============================================================================

error_msg() {
  echo -e "${1}"
}

error_msg+exit() {
  error_msg ${1}
  exit 1
}

restart_dspace_container(){
  echo -en "🔁 Restarting ${1} container...\r"
  docker container restart "${1}" >> /dev/null
  if [ $? -ne 0 ]
  then
    error_msg+exit "\033[K❌ Error when restarting the ${1} container";
  fi
  sleep 10 # TODO :: check if could be removed
  echo -e "\033[K✅ Restarting ${1} container"
}

create_group(){
  docker exec ${BACKEND} sh -c "\
      /dspace/bin/dspace dsrun org.dspace.uclouvain.administer.GroupManagement \
      --action create \
      --name ${1}" >> "${LOG_PATH}"
  if [ $? -ne 0 ]
  then
      error_msg+exit "\t❌ Error creating '${1}' group !"
  fi
  echo -e "\t${CYAN}${1}${NC} group created"
}


# =============================================================================
#     MAIN SCRIPT
# =============================================================================

# Setup script to initialize a new backend docker stack and insert basic data.
# STEPS:
#   0. Check script requirement
#     - Check script argument
#     - Check required external system command
#   1. Check that the backend container is running
#   2. Reinitialize the app
#     - Clearing the database and migrating it
#     - Reset data from Solr
#   3. Create users:
#     - Admin user to execute future commands
#     - Create additional users
#     - Create user groups and assign users into.
#   4. Initialize collections and communities
#   5. Register project specific metadata schemas & fields.

# Define constants used during script execution
readonly BACKEND="dspace"
readonly FILE_PATH="$(pwd)/${BASH_SOURCE[0]}"
readonly WORKING_PATH=$(dirname -- "${FILE_PATH}")
readonly LOG_FILE=$(date +"%Y-%m-%d_%T")
readonly LOG_PATH="${WORKING_PATH}/log/${LOG_FILE}.log"
readonly USERS_CONFIG_PATH="${WORKING_PATH}/config/users.json"
readonly PERMISSIONS_FILE="${WORKING_PATH}/config/permissions.json"
readonly SOLR_BASE_URL="http://localhost:8983/solr"  # without ending slash !!
readonly REQUIRED_EXTERNAL_COMMAND=( jq )

# Colored message
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Global script variable
CLEAN_SOLR_CORES=false

# STEP#0 :: Check script requirement ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#   - Check script arguments
#   - Check all required commands for this script exists and are installed on the system.
#   - Create log file
while [ $# -gt 0 ]
do
  case $1 in
    -C|--clean-solr)
      CLEAN_SOLR_CORES=true;;
    (-*)
      error_msg+exit "$0: Unrecognized option $1";;
    (*)
      break;;
  esac
  shift
done

for cmd in "${REQUIRED_EXTERNAL_COMMAND[@]}"
do
	if ! command -v "$cmd" &> /dev/null
  then
      error_msg+exit  "⛔ \`${RED}${cmd}${NC}\` could not be found. This command is required to complete the script."
      exit 1
  fi
done

# Create the log file if it does not exists
[ ! -d "${WORKING_PATH}/log" ] && mkdir "${WORKING_PATH}/log"
echo -e "📃 Logs for this execution can be found in: ${CYAN}${LOG_PATH}${NC}"
touch "${LOG_PATH}"


# STEP#1 :: Check dspace container ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#    Check that the backend container is running, if not, we can stop execution
#    of the script and return an explicit exit code.
docker container inspect ${BACKEND} >> "${LOG_PATH}"
if [ $? -ne 0 ]
then
  error_msg+exit "⛔ The given container does not exists ⛔"
fi
echo "✅ Found the container, performing actions ..."


# STEP#2: Cleaning old data ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#    Reinitialize the app by clearing the database and migrating it
#    Clean the Solr cores from old data
echo -e "♻️  Cleaning data ..."
echo -e "\t🗄️  database..."
docker exec ${BACKEND} sh -c "(yes | /dspace/bin/dspace database clean) && /dspace/bin/dspace database migrate" >> "${LOG_PATH}"
if [ $? -ne 0 ]
then
    error_msg+exit "\033[K❌ Error while cleaning the database, aborting..."
fi

if ${CLEAN_SOLR_CORES}
then
  echo -e "\t🗄️  Solr cores..."
  solr_core_names=$(curl -s ${SOLR_BASE_URL}/admin/cores?action=STATUS | jq '.status' | jq -r 'keys | @sh' | tr -d \')
  for core_name in $solr_core_names
  do
    solr_core_url="${SOLR_BASE_URL}/${core_name}/update?commit=true"
    curl -s "$solr_core_url" -H "Content-type: text/xml" --data-binary '<delete><query>*:*</query></delete>' > /dev/null
    echo -e "\t\t🗑  ${CYAN}${core_name}${NC} core cleared"
  done
fi
restart_dspace_container ${BACKEND}


# STEP#3: Create users ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#   * Extract all groups to assign to user and create them
#   * Create additional groups
#   * Add an admin user
#   * Adds users and assign it to the correct group
echo -e "👥 Creating user groups..."
groups=$(jq '.users[].groups | @sh' ${USERS_CONFIG_PATH} | tr -d \' | tr -d \" | awk '{OFS="\n"; $1=$1}1' | sort -u | tr '\n' ",")
IFS=',' groups=($groups)
for group in "${groups[@]}"
do
  create_group "${group}"
done
create_group "UCLouvain network"


echo -e "👤 Creating admin user..."
users_number=$(jq '.admins | length' "${USERS_CONFIG_PATH}")
for (( i=0; i<users_number; i++))
do
  email=$(jq .admins[$i].email "${USERS_CONFIG_PATH}")
  lastname=$(jq .admins[$i].lastname "${USERS_CONFIG_PATH}")
  firstname=$(jq .admins[$i].firstname "${USERS_CONFIG_PATH}")
  password=$(jq .admins[$i].password "${USERS_CONFIG_PATH}")
  docker exec ${BACKEND} sh -c "\
      /dspace/bin/dspace create-administrator \
      --email ${email} \
      --first ${firstname} \
      --last ${lastname} \
      --password ${password}" >> "${LOG_PATH}"
  if [ $? -ne 0 ]
  then
     error_msg+exit "❌ Error while creating admin user !"
  fi
  # automatic end-user agreement validation
  docker exec ${BACKEND} sh -c "\
  /dspace/bin/dspace dsrun org.dspace.uclouvain.administer.UserAgreementManagement\
  --user ${email}\
  --enable" >> "${LOG_PATH}"
  if [ $? -ne 0 ]
  then
     error_msg "\t⚠️ Error during automatic user agreement validation for '${email}'!"
  fi
  echo -e "\t${CYAN}${email}${NC} admin created"
done
ADMIN_EMAIL=$(jq .admins[0].email "${USERS_CONFIG_PATH}")

echo -e "👤 Creating alt users..."
users_number=$(jq '.users | length' "${USERS_CONFIG_PATH}")
for (( i=0; i<users_number; i++))
do
    email=$(jq .users[$i].email "${USERS_CONFIG_PATH}")
    lastname=$(jq .users[$i].lastname "${USERS_CONFIG_PATH}")
    firstname=$(jq .users[$i].firstname "${USERS_CONFIG_PATH}")
    password=$(jq .users[$i].password "${USERS_CONFIG_PATH}")
    docker exec ${BACKEND} sh -c "\
        /dspace/bin/dspace user --add \
        --email ${email} \
        --givenname ${firstname} \
        --surname ${lastname} \
        --password ${password}" >> "${LOG_PATH}"
    if [ $? -ne 0 ]
    then
        error_msg+exit "\t❌ Error creating '${email}' user !"
    fi
    groups=$(jq -r ".users[${i}].groups[] | @sh" ${USERS_CONFIG_PATH} | tr -d \" | tr -d \' | tr '\n' ",")
    for group in $groups
    do
      docker exec ${BACKEND} sh -c "\
      /dspace/bin/dspace dsrun org.dspace.uclouvain.administer.UserGroupManagement\
      --user ${email}\
      --group ${group}\
      --action add" >> "${LOG_PATH}"
      if [ $? -ne 0 ]
      then
          error_msg+exit "\t❌ Error assigning '${email}' to '${group}' !"
      fi
    done
    # automatic end-user agreement validation
    docker exec ${BACKEND} sh -c "\
    /dspace/bin/dspace dsrun org.dspace.uclouvain.administer.UserAgreementManagement\
    --user ${email}\
    --enable" >> "${LOG_PATH}"
    if [ $? -ne 0 ]
    then
       error_msg "\t⚠️ Error during automatic user agreement validation for '${email}'!"
    fi
    echo -e "\t${CYAN}${email}${NC} user created"
done

# STEP#4: Create basics communities & collections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo -en "📘 Creating new community and collection...\r"
docker exec ${BACKEND} sh -c "\
    /dspace/bin/dspace structure-builder \
    -e ${ADMIN_EMAIL} \
    -f /dspace/config/init-sample.xml \
    -o /etc/null" >> "${LOG_PATH}"
if [ $? -ne 0 ]
then
    error_msg+exit "\033[K❌ Error while creating community and collection !"
fi
echo -e "\033[K✅ Community and collection created"

# STEP#5: Register specific schemas & metadata registries ~~~~~~~~~~~~~~~~~~~~~
echo -e "📘 Registering schemas & metadata fields..."
METADATA_REGISTRIES=(
  "registries/dc-types.xml"
  "registries/authors-types.xml"
  "registries/tag-types.xml"
)
for registry in "${METADATA_REGISTRIES[@]}"
do
  docker exec ${BACKEND} sh -c "\
      /dspace/bin/dspace registry-loader -metadata \
      /uclouvain/config/${registry}" >> "${LOG_PATH}"
  if [ $? -ne 0 ]
  then
      error_msg+exit "❌ Error during ${registry} creation !"
  fi
  echo -e "\t${CYAN}${registry}${NC} registered"
done

# STEP#5: Permissions management ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
echo -e "🔒 Permissions management..."
collection_length=$(jq '.collections | length' "${PERMISSIONS_FILE}")
for (( i=0; i<collection_length; i++))
do
  collection_name=$(jq .collections[$i].collection_name "${PERMISSIONS_FILE}")
  permissions_length=$(jq ".collections[${i}].permissions | length" "${PERMISSIONS_FILE}")
  for (( j=0; j<permissions_length; j++))
  do
    workflow_role=$(jq .collections[${i}].permissions[$j].type "${PERMISSIONS_FILE}")
    groups=$(jq ".collections[${i}].permissions[$j].groups[] | @sh" "${PERMISSIONS_FILE}" \
             | tr -d \' | tr -d \" | awk '{OFS="\n"; $1=$1}1' | sort -u | tr '\n' ",")
    IFS=',' groups=($groups)
    for group_name in "${groups[@]}"
    do
      echo -en "\tAssigning ${CYAN}${group_name}${NC} to ${CYAN}${collection_name}${NC}.${CYAN}${workflow_role}${NC}..."
      docker exec ${BACKEND} sh -c "\
            /dspace/bin/dspace dsrun org.dspace.uclouvain.administer.CollectionPermissionManagement \
            --enable \
            --collection ${collection_name} \
            --role ${workflow_role} \
            --group ${group_name}" >> "${LOG_PATH}"
      if [ $? -ne 0 ]
      then
          error_msg+exit "❌ Error during permission management"
      fi
      echo -e "\t${GREEN}Success${NC}"
    done
  done
done

# STEP#FINAL: Restart the container
restart_dspace_container ${BACKEND}
echo "🎉 Setup script finished successfully! Time for a drink. Enjoy ! 🍺🍺🍺"
