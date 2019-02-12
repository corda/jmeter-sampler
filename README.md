<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Corda Performance Test Sampler SDK

This is a small SDK to illustrate how to write a custom JMeter sampler for the Corda performance test suite.

## Prerequisites

Using this SDK requires
- A JDK version 1.8 
- an installed version of [Corda Enterprise](https://www.r3.com/corda-enterprise/) version 4.0 or later
- The [Corda Performance test suite](https://docs.corda.r3.com/performance-testing/toc-tree.html) to actually use the 
samplers built with this SDK.

> **Important note** Even though this SDK is available under an Apache 2 License, it requires a Corda Enterprise
> license and the Corda Enterprise performance test suite to acutally be used


## Building

Clone the git repository locally using `git clone git@github.com/corda/jmeter-sampler`. Change into the project
directory, and you should be able to build the project using `./gradlew jar` (`gradlew.bat jar` on Windows).
This should result in a jar file under `<project directory>/build/libs` that contains the built samplers.

## Code Examples

In the `src/main` section, you'll find a Kotlin and Java example of a sampler that runs flows via JMeter to gauge
the performance of a Corda installation. They are actually a copy of the `CashIssueAndPaySampler` distributed
with the performance test suite.

The sampler derives from the base class `AbstractSampler` that provides most of the scaffolding required to 
drive a Corda node via RPC flows from a JMeter sampler. The code in the classes is annotated with comments 
explaining what needs to be done to make the sampler work.

The `AbstractSampler` itself derives from `BaseFlowSampler` which implements the JMeter-provided class 
`AbstractJavaSamplerClient` that provides the interface JMeter expects in order to be able run performance
tests with a sampler. The `BaseFlowSampler` handles useful utility functions such as setting up of the RPC connection
to the node, and deals with actually invoking the flow and turning the flow duration, succes or failure into a JMeter
result.

The `AbstractBaseSampler` adds helper methods on top for retrieving the notary information and handling X500Names
and Parties.

## Custom Samplers

The samplers provided with the performance test suite are hardcoded to use the Corda Enterprise performance test
CorDapp. In order to run a performance test with a different CorDapp, a custom sampler is required that has
code to invoke the flows of the CorDapp under test.

### Writing a Custom Sampler

The easiest case for a custom sampler is a sampler that works similarly to the existing example sampler, i.e.
it does some preparations once before the test begins, and then runs a specific flow for each test iteration.
The base class manages a pool of Corda RPC connections and provides an RPC proxy for each function call to
the custom class methods.

Such a class should extend the `AbstractSampler` and must implement the following methods:

* `Set<Argument> getAdditionalArgs()`: This must return a set of all JMeter 
[Arguments](https://jmeter.apache.org/api/org/apache/jmeter/config/Argument.html) that will be configurable 
from the test plan using this sampler.   
* `void setupTest(CordaRPCOps rpcProxy, JavaSamplerContext testContext)`: This method is run once before each of the
tests in a testplan starts and can be used to prepare anything that is required before the tests can start. The
`testContext` can be used to get to the arguments from the testplan. The `rpcProxy` allows to make RPC calls to the
Corda node that will be used to drive the tests.
* `FlowInvoke<> creatFlowInvoke(CordaRPCOps rpcProxy, JavaSamplerContext testContext)`: This function must return
a `FlowInvoke` object that can be used to invoke the flow that shall be run as part of the performance test. In
this method, the `rpcProxy` gives full access to the node, however it should be noted that any run time in here
counts towards the sample's latency (i.e. setup time), not the sample run time. The base sampler will invoke 
the flow after this method returns and count the response time of the flow as sample run time.
* `void teardownTest(CordaRPCOps rpcProxy, JavaSamplerContext testContext)`: This function will be run after all 
the tests are done and can be used for clean up or to release any resources. However, note that this is only run
at the end of the whole testplan, not after each sub test in the testplan. This is a shortcoming of JMeter itself and
out of our control.

Optionally, the custom sampler can also override the method 
`void additionalFlowResponseProcessing(JavaSamplerContext context, SampleResult sample, Object response )`
in order to enrich the sample result with any information from the flow response. The default of this method
is a no op, overriding it is not required.

Any additional dependencies of the custom sampler (e.g. the CorDapp to be tested) need to be added to the
build.gradle file. In our example, we had to add the `corda-finance` CorDapp in order to write the 
`CashIssueSampler` that uses this CorDapp.

Any sampler that drives Corda in a fundamental different way needs to implement `AbstractJavaSamplerClient`
itself and handle all the Corda details in custom code. Support for different kinds of samplers might be added
in the future - please contact your Corda support representative at R3 if you are planning to do this.

 
### Running a Custom Sampler

To use a custom sampler, it needs to be loaded into JMeter so it can be used in a testplan. 
Additional sampler jars can be added to tJMeter Corda using the `-XadditionalSearchPaths=<path to jar file>`
command line argument, along with any JARs/classes it depends on.

When using JMeter server instances for remote invocations, it is crucial that the same additional jar gets loaded
in the client instance and all server intances that this client connects to. See the [performance suite
documentation](https://docs.corda.r3.com/performance-testing/jmeter-samplers.html#custom-sampler-clients) 
for details how to set up a custom sampler jar.