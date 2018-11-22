## Skeleton constituent

### Setup project
This section helps you get started with setting up this constituent in eclipse

- Clone repository on to your machine as
```shell
git clone git@repo.haystack.one:server.tachyon/constituent.shell.git
```
- Move to directory, and add plugins.sbt as,
```shell
vi project/plugins.sbt
```
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



### Build configurations
Helps us train and deploy locally.

######Simulating `pio train`
Create a new _Run/Debug Configuration_ by going to _Debug > Edit Configurations...._ Choose _Scala Application_ and click on the + button. Name it pio train and put in the following:

- Main class:
    - `org.apache.predictionio.workflow.CreateWorkflow`
- VM options:
    - `-Dspark.master=local -Dlog4j.configuration=file:/<your_pio_path>/conf/log4j.properties -Dpio.log.dir=<path_of_log_file>`
- Program arguments:
    - `--engine-id dummy --engine-version dummy --engine-variant engine.json --env dummy=dummy`

Make sure working directory is set to the base directory of the template that you are working on.

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
