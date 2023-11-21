#!/bin/bash

# Setup script to initilialize a new backend docker stack and insert basic data. 
# STEPS:
# 1. Check that the backend container is running 
# 2. Reinitialize the app by clearing the database and migrating 
# 3. Create an admin user to execute future commands 
# 4. Initilialize collections and communities 
# 5. Create additionnal users 
# 6. Execute the Python script to make the changes that are not possible via the CLI

readonly BACKEND="dspace"

readonly FILE_PATH="$(pwd)/${BASH_SOURCE[0]}"
readonly WORKING_PATH=$(dirname -- "${FILE_PATH}")
readonly LOG_FILE=$(date +"%Y-%m-%d_%T")
readonly LOG_PATH="${WORKING_PATH}/log/${LOG_FILE}.log"

# Create the log file if it does not exists 
[ ! -d "${WORKING_PATH}/log" ] && mkdir "${WORKING_PATH}/log"

echo "Logs for this execution can be find in: ${LOG_PATH}"

touch ${LOG_PATH}

# Path to the config files 
readonly USERS_CONFIG_PATH="${WORKING_PATH}/config/users.json"
readonly GROUPS_CONFIG_PATH="${WORKING_PATH}/config/groups.json"
readonly SCHEMAS_CONFIG_PATH="${WORKING_PATH}/config/schemas.json"
readonly COLLECTIONS_CONFIG_PATH="${WORKING_PATH}/config/collections.json"


docker container inspect ${BACKEND} >> ${LOG_PATH}
if [ $? -eq 0 ]
then 
    echo "✅ Found the container, performing actions ..."

    # STEP 1: CLEAN DATABASE

    echo -en "♻️  Cleaning database data...\r"
    docker exec ${BACKEND} sh -c "(yes | /dspace/bin/dspace database clean) && /dspace/bin/dspace database migrate" >> ${LOG_PATH}
    if [ $? -eq 0 ]
    then        
        docker container restart ${BACKEND} >> ${LOG_PATH}
        echo -e "\033[K✅ Cleaning successful !"
    else
        echo "\033[K❌ Error while cleaning the database, aborting..."
        exit 1
    fi 

    echo -en "🔁 Restarting the backend container...\r"
    sleep 20
    echo -e "\033[K✅ Restarting the backend container"

    # STEP 2: INITIALIZE ADMIN USER

    echo -en "👤 Creating admin user...\r"

    admin_email=$(jq '.admin_login.email' ${USERS_CONFIG_PATH})
    admin_name=$(jq '.admin_login.name' ${USERS_CONFIG_PATH})
    admin_password=$(jq '.admin_login.pwd' ${USERS_CONFIG_PATH}) 

    docker exec ${BACKEND} sh -c "\
        /dspace/bin/dspace create-administrator -e ${admin_email} -f ${admin_name} -l ${admin_name} -p ${admin_password}" \
        >> ${LOG_PATH}

    if [ $? -eq 0 ]
    then
        echo -e "\033[K✅ Admin created"
    else
        echo -e "\033[K❌ Error while creating admin user ! (maybe it already exists ?)"
        exit 1
    fi

    # STEP 3: INIT COLLECTIONS & COMMUNITIES

    echo -en "📘 Creating new community and collection...\r"
    docker exec ${BACKEND} sh -c "/dspace/bin/dspace structure-builder -e ${admin_email} -f /dspace/config/init-sample.xml -o /etc/null" >> ${LOG_PATH}
    if [ $? -eq 0 ]
    then
        echo -e "\033[K✅ Community and collection created"
    else
        echo -e "\033[K❌ Error while creating community and collection !"
        exit 1
    fi

    # STEP 4: ADD ALT USERS (STUDENT, LIBRARIAN & MANAGER)

    echo -en "👤 Creating alt users..\r"
    
    # Using the setup.json config file 
    users_number=$(jq '.users | length' ${USERS_CONFIG_PATH})
    users_creation_error=false

    # Looping trough the users
    for (( i=0; i<$users_number; i++))
    do 
        email=$(jq .users[$i].email ${USERS_CONFIG_PATH})
        password=$(jq .users[$i].pwd ${USERS_CONFIG_PATH})
        name=$(jq .users[$i].group ${USERS_CONFIG_PATH})

        docker exec ${BACKEND} sh -c "/dspace/bin/dspace user --add --email ${email} -g ${name} -s ${name} --password ${password}" >> ${LOG_PATH}
        
        if [ $? -ne 0 ]
        then
            users_creation_error=true
        fi
    done
    
    if ! $users_creation_error
    then
        echo -e "\033[K✅ Alt users created"
    else
        echo -e "\033[K❌ Error while creating alt users !"
        exit 1
    fi

    # STEP 5: INITIALIZE NEW GROUPS, METADATA REGISTERS & MANAGE COLLECTION RIGHTS(Python script)
    
    echo "👤 Creating user groups, metadata registers and managing collection rights..."

    python ${WORKING_PATH}/setup.py >> ${LOG_PATH}

    if [ $? -eq 0 ]
    then
        echo "✅ Done!"
    else
        echo "❌ Error while executing the python script !"
        exit 1
    fi

    echo "🎉 Setup script finished successfully! 🎉"
else
    echo "⛔ The given container does not exists ⛔"
fi