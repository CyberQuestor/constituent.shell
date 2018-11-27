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

### Documentation usage

Use this structure for other constituents

##### 1 Recommendations

###### 1.1 Event data
- user *view* context events
- user *interested* context events

###### 1.2 Input query
- user ID

###### 1.3 Output predicted result
- a ranked list of recommended contextIDs

###### 1.4 Training methodology
- ALS

###### 1.5 Example usage (capabilities)
- Person view this, bought that and hence might be interested in this item

### Versions

#### v0.13.0

Latest from for Apache PredictionIO 0.13.0
