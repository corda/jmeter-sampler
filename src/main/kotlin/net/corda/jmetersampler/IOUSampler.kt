package net.corda.jmetersampler

import com.example.flow.ExampleFlow
import com.r3.corda.jmeter.AbstractSampler
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import org.apache.jmeter.config.Argument
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext

class IOUSampler : AbstractSampler() {
    companion object {
        val otherParty = Argument("otherPartyName", "", "<meta>", "The X500 name of the payee.")
        val amount = Argument("IouAmount", "50", "<meta>", "How many USD do we owe?")
    }

    private lateinit var counterParty: Party
    private var iouAmount = 50

    override fun createFlowInvoke(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext): FlowInvoke<*> {
        return FlowInvoke<ExampleFlow.Initiator>(ExampleFlow.Initiator::class.java, arrayOf(iouAmount, counterParty))
    }

    override fun setupTest(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext) {
        // this method initialises the notary field on the base class - not all samplers might need a notary, so it has
        // to be called explicitly if you want to use the notary field.
        getNotaryIdentity(rpcProxy, testContext)

        // This is a generic helper to turn an X500 name into a Corda identity via RPC
        counterParty = getIdentity(rpcProxy, testContext, otherParty)

        // this is how values from sampler arguments are read - the test context can extract the value
        iouAmount = testContext.getParameter(amount.name, amount.value)!!.toInt()
    }

    override fun teardownTest(rpcProxy: CordaRPCOps, testContext: JavaSamplerContext) {
    }

    override val additionalArgs: Set<Argument>
        get() = setOf(AbstractSampler.notary, otherParty, amount)
}