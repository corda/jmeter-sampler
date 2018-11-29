package net.corda.jmetersampler

import com.r3.corda.enterprise.perftestcordapp.POUNDS
import com.r3.corda.enterprise.perftestcordapp.flows.CashIssueAndPaymentFlow
import com.r3.corda.enterprise.perftestcordapp.flows.CashIssueAndPaymentNoSelection
import com.r3.corda.jmeter.AbstractSampler
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.OpaqueBytes
import org.apache.jmeter.config.Argument
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext
import org.apache.jmeter.samplers.SampleResult

/**
 * This class is a copy of the CashIssueAndPay sampler available in the JMeter Corda package in the performance test suite.
 * It is a good example for a relatively simple sampler invoking a CorDapp flow via RPC.
 *
 * Note that it derives from AbstractSampler - this is a base class that provides the basic JMeter integration and a few
 * helper methods that are required to run a CorDapp flow.
 * Anything that is specific to the CorDapp or the flow should go into the derived class.
 */
class ExampleSamplerKotlin : AbstractSampler() {
    companion object JMeterProperties {
        // we need to create an argument instance for each test plan argument we are expecting
        val otherParty = Argument("otherPartyName", "", "<meta>", "The X500 name of the payee.")
        val coinSelection = Argument("useCoinSelection", "false", "<meta>", "True to use coin selection and false (or anything else) to avoid coin selection.")
        val anonymousIdentities = Argument("anonymousIdentities", "false", "<meta>", "True to use anonymous identities and false (or anything else) to use well known identities.")
    }

    lateinit var counterParty: Party
    var useCoinSelection: Boolean = true
    var useAnonymousIdentities: Boolean = true

    /**
     * This set-up method is called once before the tests start and can be used to initialise values that will be required
     * for the tests.
     */
    override fun setupTest(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext) {
        // this method initialises the notary field on the base class - not all samplers might need a notary, so it has
        // to be called explicitly if you want to use the notary field.
        getNotaryIdentity(rpcProxy, testContext)

        // This is a generic helper to turn an X500 name into a Corda identity via RPC
        counterParty = getIdentity(rpcProxy, testContext, otherParty)

        // this is how values from sampler arguments are read - the test context can extract the value
        useCoinSelection = testContext.getParameter(coinSelection.name, coinSelection.value)!!.toBoolean()
        useAnonymousIdentities = testContext.getParameter(anonymousIdentities.name, anonymousIdentities.value)!!.toBoolean()
    }

    /**
     * This method gets called to create the flow invoke to run the CorDapp - this method gets called for every run of
     * the sampler - the flow invoke class is then used to invoke the flow via RPC and time how long it takes to complete.
     */
    override fun createFlowInvoke(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext): FlowInvoke<*> {
        val amount = 2_000_000.POUNDS
        if (useCoinSelection) {
            return FlowInvoke<CashIssueAndPaymentFlow>(CashIssueAndPaymentFlow::class.java, arrayOf(amount, OpaqueBytes.of(1), counterParty, useAnonymousIdentities, notaryIdentity))
        } else {
            return FlowInvoke<CashIssueAndPaymentNoSelection>(CashIssueAndPaymentNoSelection::class.java, arrayOf(amount, OpaqueBytes.of(1), counterParty, useAnonymousIdentities, notaryIdentity))
        }
    }

    /**
     * Any clean-up task that needs doing should go here - beware that this is only run after all tests in one testplan
     * are done, so this is no good to release resources that might be required by subsequent tests.
     */
    override fun teardownTest(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext) {
    }

    /**
     * This is the list of arguments that we expect to get from the testplan - anything returned here appears as a
     * parameter in the testplan and can be configured on a per test basis. Note that the notary Argument is defined
     * on the AbstractSampler class, but still needs to be included here if we plan to use a notary.
     */
    override val additionalArgs: Set<Argument>
        get() = setOf(AbstractSampler.notary, otherParty, coinSelection, anonymousIdentities)

    /**
     * This method gets invoked after each sample result has been collected and allows to add extra information to the
     * sample. Overriding it is optional, by default it is a no op.
     */
    override fun additionalFlowResponseProcessing(context: JavaSamplerContext, sample: SampleResult, response: Any?) {
        // Optionally add data from the response to the sample (e.g. performance figures the flow has collected)
    }
}