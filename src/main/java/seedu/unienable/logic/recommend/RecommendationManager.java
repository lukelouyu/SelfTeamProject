package seedu.unienable.logic.recommend;

import java.util.Optional;

import seedu.unienable.model.recommend.RecommendationProposal;

/** Holds the current in-memory recommendation proposal for this application run only. */
public class RecommendationManager {
    private RecommendationProposal proposal;

    /** Stores the current proposal, replacing any earlier one. */
    public void setProposal(RecommendationProposal proposal) {
        this.proposal = proposal;
    }

    /** Returns the current proposal, if any. */
    public Optional<RecommendationProposal> getProposal() {
        return Optional.ofNullable(proposal);
    }

    /** Returns whether a proposal currently exists. */
    public boolean hasProposal() {
        return proposal != null;
    }

    /** Clears the current proposal. */
    public void clear() {
        proposal = null;
    }
}
