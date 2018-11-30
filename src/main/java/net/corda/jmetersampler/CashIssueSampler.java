package net.corda.jmetersampler;

import com.r3.corda.jmeter.AbstractSampler;
import net.corda.core.contracts.Amount;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.CashIssueFlow;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.HashSet;
import java.util.Set;

import static com.r3.corda.enterprise.perftestcordapp.Currencies.DOLLARS;

/**
 * A sampler client using a different CorDapp (the corda finance CorDapp in this case). Note that for running a test
 * with this sampler, JMeter will need the CorDapp itself on its class path.
 */
public class CashIssueSampler extends AbstractSampler {
    /**
     * The only required argument is the notary
     */
    @NotNull
    @Override
    public Set<Argument> getAdditionalArgs() {
        Set<Argument> arguments = new HashSet<>();
        arguments.add(AbstractSampler.getNotary());
        return arguments;
    }

    /**
     * Create a flow invoke for the net.corda.finance.flows.CashIssueFlow with a fixed amount.
     * Making the amount configurable from the testplan might be a good exercise for the reader.
     */
    @NotNull
    @Override
    public FlowInvoke<?> createFlowInvoke(CordaRPCOps rpcProxy, JavaSamplerContext testContext) {
        Amount<Currency> amount = DOLLARS(100000);
        return new FlowInvoke<>(CashIssueFlow.class, new Object[]{amount, OpaqueBytes.of(new Byte("1")), notaryIdentity});
    }

    /**
     * If we need the notary, we need to make sure it gets populated - remember the AbstractSampler has the code
     * to do this, but does not call it by default.
     */
    @Override
    public void setupTest(CordaRPCOps rpcProxy, JavaSamplerContext testContext) {
        getNotaryIdentity(rpcProxy, testContext);
    }

    /**
     * Nothing to tear down for this simple sampler.
     */
    @Override
    public void teardownTest(CordaRPCOps rpcProxy, JavaSamplerContext testContext) {

    }
}
