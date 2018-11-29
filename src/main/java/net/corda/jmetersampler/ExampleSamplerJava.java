package net.corda.jmetersampler;

import com.r3.corda.enterprise.perftestcordapp.flows.CashIssueAndPaymentFlow;
import com.r3.corda.enterprise.perftestcordapp.flows.CashIssueAndPaymentNoSelection;
import com.r3.corda.jmeter.AbstractSampler;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.OpaqueBytes;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.r3.corda.enterprise.perftestcordapp.Currencies.POUNDS;

public class ExampleSamplerJava extends AbstractSampler {
    // we need to create an argument instance for each test plan argument we are expecting
    static private Argument otherParty = new Argument("otherPartyName", "", "<meta>", "The X500 name of the payee.");
    static private Argument coinSelection = new Argument("useCoinSelection", "false", "<meta>", "True to use coin selection and false (or anything else) to avoid coin selection.");
    static private Argument anonymousIdentities = new Argument("anonymousIdentities", "false", "<meta>", "True to use anonymous identities and false (or anything else) to use well known identities.");


    private Party counterParty;
    private Boolean useCoinSelection = true;
    private Boolean useAnonymousIdentities = true;

    /**
     * This set-up method is called once before the tests start and can be used to initialise values that will be required
     * for the tests.
     */
    @Override
    public void setupTest(CordaRPCOps rpcProxy, JavaSamplerContext testContext) {
        // this method initialises the notary field on the base class - not all samplers might need a notary, so it has
        // to be called explicitly if you want to use the notary field.
        getNotaryIdentity(rpcProxy, testContext);

        // This is a generic helper to turn an X500 name into a Corda identity via RPC
        counterParty = getIdentity(rpcProxy, testContext, otherParty);

        // this is how values from sampler arguments are read - the test context can extract the value
        useCoinSelection = Boolean.valueOf(testContext.getParameter(coinSelection.getName(), coinSelection.getValue()));
        useAnonymousIdentities = Boolean.valueOf(testContext.getParameter(anonymousIdentities.getName(), anonymousIdentities.getValue()));
    }

    /**
     * This method gets called to create the flow invoke to run the CorDapp - this method gets called for every run of
     * the sampler - the flow invoke class is then used to invoke the flow via RPC and time how long it takes to complete.
     */
    @NotNull
    @Override
    public FlowInvoke<?> createFlowInvoke(CordaRPCOps rpcProxy, JavaSamplerContext testContext) {
        Amount<Currency> amount = POUNDS(2_000_000);
        if (useCoinSelection) {
            return new FlowInvoke<>(CashIssueAndPaymentFlow.class, new Object[]{amount, OpaqueBytes.of(new Byte("1")), counterParty, useAnonymousIdentities, notaryIdentity});
        } else {
            return new FlowInvoke<>(CashIssueAndPaymentNoSelection.class, new Object[]{amount, OpaqueBytes.of(new Byte("1")), counterParty, useAnonymousIdentities, notaryIdentity});
        }
    }

    /**
     * Any clean-up task that needs doing should go here - beware that this is only run after all tests in one testplan
     * are done, so this is no good to release resources that might be required by subsequent tests.
     */
    @Override
    public void teardownTest(CordaRPCOps rpcProxy, JavaSamplerContext testContext) {
    }

    /**
     * This is the list of arguments that we expect to get from the testplan - anything returned here appears as a
     * parameter in the testplan and can be configured on a per test basis. Note that the notary Argument is defined
     * on the AbstractSampler class, but still needs to be included here if we plan to use a notary.
     */
    @NotNull
    @Override
    public Set<Argument> getAdditionalArgs() {
        return Stream.of(AbstractSampler.getNotary(), otherParty, coinSelection, anonymousIdentities).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * This method gets invoked after each sample result has been collected and allows to add extra information to the
     * sample. Overriding it is optional, by default it is a no op.
     */
    @Override
    protected void additionalFlowResponseProcessing(JavaSamplerContext context, SampleResult sample, Object response) {
        // Optionally add data from the response to the sample (e.g. performance figures the flow has collected)
    }
}
