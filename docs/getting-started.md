Getting started
===============

Adding the dependency
---------------------

To use this library in a Maven project, add the following to your `pom.xml`:

```xml

<dependency>
    <groupId>nl.datastations</groupId>
    <artifactId>dans-java-utils</artifactId>
    <version>{{ project_version }}</version>
</dependency>
```

Basic usage
-----------

### Example: ExecutorServiceFactory

In the configuration class of your DropWizard project, declare a field for configuring the executor. If your executor processes jobs, you could name it
`jobQueue`.

```java
public class MyAppConfiguration extends Configuration {

    private ExecutorServiceFactory jobQueue;

    public void setJobQueue(ExecutorServiceFactory jobQueue) {
        this.jobQueue = jobQueue;
    }

    public ExecutorServiceFactory getJobQueue() {
        return jobQueue;
    }

    // ...
}
```

In the configuration file you can now configure the jobQueue as follows:

```yaml
jobQueue:
  nameFormat: "job-queue-thread-%d"

  maxQueueSize: 4
  # Number of threads will be increased when maxQueueSize is exceeded.
  minThreads: 2
  # No more than maxThreads will be created, though
  maxThreads: 10
  # Threads will die after 60 seconds of idleness
  keepAliveTime: 60 seconds
```

In your Application class' `run` method you can finally create the actual executor:

```java
// inside run()
ExecutorService executor = configuration.getJobQueue().build(environment);
```

and pass it to the resources or other components that need to use it.
