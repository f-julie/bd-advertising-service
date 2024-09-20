package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
        /*List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();
        boolean allTruePredicates = true;
        for (TargetingPredicate predicate : targetingPredicates) {
            TargetingPredicateResult predicateResult = predicate.evaluate(requestContext);
            if (!predicateResult.isTrue()) {
                allTruePredicates = false;
                break;
            }
        }

        return allTruePredicates ? TargetingPredicateResult.TRUE :
                                   TargetingPredicateResult.FALSE;*/

        List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();

        // Create a fixed thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(10); // Adjust the pool size as needed
        List<Future<TargetingPredicateResult>> futures = new ArrayList<>();

        try {
            // Submit tasks for each predicate
            for (TargetingPredicate predicate : targetingPredicates) {
                Callable<TargetingPredicateResult> task = () -> predicate.evaluate(requestContext);
                futures.add(executorService.submit(task));
            }

            // Check if all predicates are true
            boolean allTruePredicates = true;
            for (Future<TargetingPredicateResult> future : futures) {
                try {
                    if (!future.get().isTrue()) {
                        allTruePredicates = false;
                        break; // Exit early if one is false
                    }
                } catch (Exception e) {
                    // Handle exceptions for individual predicate evaluations
                    allTruePredicates = false;
                    break;
                }
            }

            //boolean allTruePredicates = targetingPredicates.stream()  // Create a stream from the list
            //        .allMatch(predicate -> predicate.evaluate(requestContext).isTrue()); // Check if all predicates are true

            return allTruePredicates ? TargetingPredicateResult.TRUE :
                    TargetingPredicateResult.FALSE;
        } finally {
            // Shutdown the executor service
            executorService.shutdown();
        }
    }
}