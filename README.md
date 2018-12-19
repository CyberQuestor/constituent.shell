## Skeleton constituent

### Setup project
This section helps you get started with setting up this constituent in eclipse

- Clone repository on to your machine as
    - `git clone git@repo.haystack.one:server.tachyon/constituent.shell.git`
- Move to directory, and add plugins.sbt as,
    - `vi project/plugins.sbt`
- Add eclipse plugin line and close.
    - `addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")`
- Issue `sbt eclipse` and wait for creation of project.
- Import project in eclipse.

### Import storage classes
You will have to import following storage classes to your project. These JARs can be found inside the `dist/lib` and `dist/lib/spark` directory of your PredictionIO source installation directory since we build from sources.

- `pio-assembly-0.13.0.jar`
- `pio-data-elasticsearch-assembly-0.13.0.jar`
- `pio-data-hbase-assembly-0.13.0.jar`
- `pio-data-hdfs-assembly-0.13.0.jar`

You may hit Scala version compatibility issues. If so, go to project compiler settings and choose _Latest 2.11 bundle (dynamic)_

Finally, edit `build.sbt` and add the following under `libraryDependencies`

- `"org.xerial.snappy" % "snappy-java" % "1.1.1.7"`
- Add this for debugging only. Do not commit it.

### Build configurations
Helps us train and deploy locally.

##### Simulating `pio train`
Create a new _Run/Debug Configuration_ by going to _Debug > Edit Configurations...._ Choose _Scala Application_ and click on the + button. Name it pio train and put in the following:

- Main class:
    - `org.apache.predictionio.workflow.CreateWorkflow`
- VM options:
    - `-Dspark.master=local -Dlog4j.configuration=file:/usr/local/pio/conf/log4j.properties -Dpio.log.dir=/var/log/haystack/pio`
- Program arguments:
    - `--engine-id dummy --engine-version dummy --engine-variant engine.json --env dummy=dummy`

Make sure working directory is set to the base directory of the template that you are working on.

Add environment variables to this configuration as indicated under deployment document; you may find it at `/usr/local/pio/conf/pio-env.sh`. Other considerations are as outlined below;

- You might encounter issues with HDFS if configuration is not set right; if so switch to file system for model storage like so;
    - `PIO_STORAGE_REPOSITORIES_MODELDATA_SOURCE: LOCALFS`
    - `PIO_STORAGE_SOURCES_LOCALFS_TYPE: localfs`
    - `PIO_STORAGE_SOURCES_LOCALFS_PATH: /path/to/data/folder`
- You might have to also do the following for smoother provisioning;
    - Copy `/usr/local/pio/conf/` to `jars` folder (the one where you brought in all the libraries from)
    - Add this folder as an _external class path_ for the project

##### Simulating `pio deploy`
Simply duplicate the previous configuration and replace the following;

- Main class:
    - `org.apache.predictionio.workflow.CreateServer`
- Program arguments:
    - `--engineInstanceId <id_from_pio_train> --engine-variant engine.json`
    - You will find `engineInstanceId` printed in console at the end of _train_

### Deployment
This section provides you details on how to provision HMLP on to HOLU. Refer to individual constituents git page for proposed port mappings and access keys. Do not forget to add access to event server as;

- Edit `/etc/default/haystack` and add base paths for event server at both announcer and consumer nodes.
    - `holu.base=http://192.168.136.90:7070`

#### Setup event pipeline
The first element is to generate access tokens denoted as prediction pipeline units.

- Execute the following to generate skeleton unit
    - `pio app new constituent.shell`
    - Add `--access-key` parameter if you want to control key generated
        - It should be a 64-char string of the form `abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ12`
- Record unit ID and access key. You will need this later.

#### Prepare constituent
It is time to prepare constituent unit files that eventually manifests as a HML pipeline.

- Retrieve engine files by cloning relevant git repository
    - `cd /var/lib/haystack/pio/constituents/`
    -  `git clone git@repo.haystack.one:server.tachyon/constituent.shell.git constituent.shell`
    - `cd constituent.shell`
- Change `appName` at `engine.json` to `constituent.shell`
- Edit `/etc/default/haystack` and add access keys to denote addition of HMLP.
    - For **consumer** nodes;
        - `haystack.tachyon.events.dispatch.skeleton=<accesskey>`
- Complete events import through migration and turning on concomitant consumer

#### Initiate first time training and deploy
It is important to complete at least one iteration of build, train and deploy cycle prior to consumption.

- Build the prediction unit as,
    - `pio build --verbose`
- Train the predictive model as (ensure events migration is complete),
    - `pio train --verbose -v engine.json -- --master spark://monad-dev-vm3:7077 --executor-memory 2G --driver-memory 1G --total-executor-cores 2`
- Deploy the prediction unit as,
    - `mkdir -p /var/log/haystack/pio/deploy/`
    - `vi /var/log/haystack/pio/deploy/17071.log`; save and close
    - `nohup pio deploy -v engine.json --ip 192.168.136.90 --port 17071 --event-server-port 7070 --feedback --accesskey <access_key> -- --master spark://monad-dev-vm3:7077 --executor-memory 2G --driver-memory 1G --total-executor-cores 2 > /var/log/haystack/pio/deploy/17071.log &`
    - Do not kill the deployed process. Subsequent train and deploy would take care of provisioning it again.
    - You can verify deployed HMLP by visiting `http://192.168.136.90:17071/` and querying at `http://192.168.136.90:17071/queries.json `
- Edit `/etc/default/haystack` and add url keys to denote addition of HMLP.
- For **announcer** nodes;
    - `haystack.tachyon.pipeline.access.skeleton=http://192.168.136.90:17071`

#### Setup consecutive training and deploy
Now that we have successfully provisioned this HMLP; let us set it up for a periodic train-deploy cycle. Note that events are always consumed at real-time but are not accounted for until the next train cycle builds the model.

- Find the accompanying shell scripts of constituent and modify for consumption.
    - `cd /var/lib/haystack/pio/constituents/constituent.shell/src/main/resources/scripts/`
    - Time to copy these files to source scripts directory;
        - `cd ../../../../`
        - `mkdir scripts`
        - `cp src/main/resources/scripts/*.sh scripts/`
    - Rename `local.sh.template` to `local.sh`
    - Edit `local.sh` and set the following values;
        - `PIO_HOME=/usr/local/pio`
        - `LOG_DIR=/var/log/haystack/pio/cumulative/17071` (ensure that the path exists)
        - `FROM_EMAIL="info@haystack.one"` (emails are for internal notifications only)
        - `TARGET_EMAIL="masterhank05@gmail.com"` (set this to our support/ customer care email or create a notifications id)
        - `IP=192.168.136.90` - denotes HMLP for queries
    - Rename `redeploy.sh.template` to `Constituent.shell_redeployment_dev.sh`
    - Edit `Constituent.shell_redeployment_dev.sh` and set the following values;
        - `HOSTNAME=192.168.136.90` (for accessing event server)
        - `PORT=170071` - denotes HMLP port for queries
        - `ACCESSKEY=` - fill this with what was generated earlier
        - `TRAIN_MASTER="spark://monad-dev-vm3:7077"`
        - `DEPLOY_MASTER="spark://monad-dev-vm3:7077"`
    - Do not forget to make it executable;
        - `chmod +x Constituent.shell_redeployment_dev.sh `
    - Adjust spark driver and executor settings as required
    - Ensure `pio build` is run at least once before enabling this script.

Finally, setup crontab for executing these scripts. `mailutils` is used in this script. For Ubuntu, you can do `sudo update-alternatives --config mailx` and see if `/usr/bin/mail.mailutils` is selected.

- Edit crontab file as;
    - `crontab -e` for user level
    - Add the entry as;
        - `0 0,6,12,18 * * * /var/lib/haystack/pio/constituents/constituent.shell/scripts/Constituent.shell_redeployment_dev.sh >/dev/null 2>/dev/null`
        - User `man cron` to check usage
        - Manage schedules in conjunction with all other HMLPs and ensure that trains do not overlap
    - Reload to take effect
        - `sudo service cron reload`
        - Restart if needed; `sudo systemctl restart cron`

You are all set!

### Documentation usage

Use this structure for other constituents

##### 1 Skeleton

###### 1.1 Overview
- Recommend contexts to user
    - `description`: given user id; recommends relevant contexts to patron (context ids)
    - `known as`: constituent.recommend-contexts-to-user
    - `takes`: user and context entities; view, interest, disinterest events
    - `queries with` - user id
    - `returns`: list of recommended context ids
    - `answers`: at feeds page, populate **"Trending now"**
    - `works for`: patron (consumer), publisher (creator)
    - `example`: detail out an example

###### 1.2 Event data
- user *view* context events
- user *interested* context events

###### 1.3 Input query
- user ID

###### 1.4 Output predicted result
- a ranked list of recommended contextIDs

###### 1.5 Training methodology
- ALS

###### 1.6 Example usage (capabilities)
- Person view this, bought that and hence might be interested in this item

### Versions

#### v0.13.0

Latest from for Apache PredictionIO 0.13.0
