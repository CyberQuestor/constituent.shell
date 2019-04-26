## Skeleton constituent

### Example version
This pipeline is now **0.0.2**

_Target API version: **1.1.3.3**_

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
    - Setup the folder as `mkdir -p /var/lib/haystack/pio/constituents/constituent.shell`
    - Go to folder as `cd /var/lib/haystack/pio/constituents/constituent.shell`
    - Generate structure folders as; `mkdir bin conf pipeline`
    - Go to pipeline folder and get all files as
        - Either clone to pipeline as, `git clone git@repo.haystack.one:server.tachyon/constituent.shell.git pipeline`
        - Or if it is zipped, `unzip constituent.shell.zip -d pipeline`
        - Make sure that the application name is set right at; `pipeline/engine.json`. Change `appName` to `constituent.shell`.
    - Copy all scripts to bin as; `cp pipeline/src/main/resources/scripts/* bin/`
    - Copy configuration to conf as; `cp pipeline/src/main/resources/configuration/* conf/`
    - Ensure that all scripts have execute permission as; `chmod +x bin/*`
    - Get your configuration right as; `vi conf/pipeline.conf`
        - Pay attention to `HOSTNAME, HOST, ACCESS_KEY, TRAIN_MASTER, DEPLOY_MASTER, X_CORES and Y_MEMORY`
- Edit `/etc/default/haystack` and add access keys to denote addition of HMLP.
    - For **consumer** nodes;
        - `haystack.tachyon.events.dispatch.skeleton=<accesskey>`
- Complete events import through migration and turning on concomitant consumer

#### Initiate first time training and deploy
It is important to complete at least one iteration of build, train and deploy cycle prior to consumption.

- Go to folder as `cd /var/lib/haystack/pio/constituents/constituent.shell/bin`
- Build the prediction unit as,
    - `./build`
- Train the predictive model as (ensure events migration is complete),
    - `./train`
- Deploy the prediction unit as,
    - `./deploy`
    - Do not kill the deployed process. Subsequent train and deploy would take care of provisioning it again.
    - You can verify deployed HMLP by visiting `http://192.168.136.90:17071/` and querying at `http://192.168.136.90:17071/queries.json `
- Edit `/etc/default/haystack` and add url keys to denote addition of HMLP.
- For **announcer** nodes;
    - `haystack.tachyon.pipeline.access.skeleton=http://192.168.136.90:17071`

#### Setup consecutive training and deploy
Now that we have successfully provisioned this HMLP; let us set it up for a periodic train-deploy cycle. Note that events are always consumed at real-time but are not accounted for until the next train cycle builds the model.

- Find the accompanying shell scripts of constituent and modify for consumption.
    - Go to constituent directory at;
        - `cd /var/lib/haystack/pio/constituents/constituent.shell/`
    - Verify configuration is right as; `vi conf/pipeline.conf`
        - Adjust spark driver and executor settings as required
    - Do not forget to make scripts executable;
        - `chmod +x bin/*`
    - Ensure `pio build` is run at least once before enabling `cron` job.

Finally, setup crontab for executing these scripts. `mailutils` is used in this script. For Ubuntu, you can do `sudo update-alternatives --config mailx` and see if `/usr/bin/mail.mailutils` is selected.

- Edit crontab file as;
    - `crontab -e` for user level
    - Add the entry as;
        - `0 0,6,12,18 * * * /var/lib/haystack/pio/constituents/constituent.shell/bin/redeploy >/dev/null 2>/dev/null`
        - Use `man cron` to check usage
        - Manage schedules in conjunction with all other HMLPs and ensure that trains do not overlap
    - Reload to take effect (optional)
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
