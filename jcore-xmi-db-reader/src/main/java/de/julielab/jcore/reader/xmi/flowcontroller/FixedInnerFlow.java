package de.julielab.jcore.reader.xmi.flowcontroller;

import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.JCasFlow_ImplBase;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This flow is supposed to route the output CASes of the {@link de.julielab.jcore.reader.xmi.XmiDBMultiplier} in
 * a fixed, sequential manner through the aggregate engine. It just skips the first delegate - the multiplier itself - then continues with the rest.</p>
 */
public class FixedInnerFlow extends JCasFlow_ImplBase {
    private final static Logger log = LoggerFactory.getLogger(FixedInnerFlow.class);
    private int currentPosition;
    private String[] fixedFlow;

    public FixedInnerFlow(String[] fixedFlow) {
        this.fixedFlow = fixedFlow;
        this.currentPosition = 0;
    }

    public Step next() {
        Step step = null;
        for (; currentPosition < fixedFlow.length && step == null; currentPosition++) {
            String aeKey = fixedFlow[currentPosition];
            // The first analysis engine is the multiplier
            if (currentPosition > 0) {
                log.trace("Inner next AE is: " + aeKey);
                step = new SimpleStep(aeKey);
            }
        }
        if (step == null) {
            // no appropriate AEs to call - end of flow
            log.trace("Inner flow Complete.");
        }
        return step == null ? new FinalStep() : step;
    }
}
