import java.util.*;

public class Issue {
    private int issueRate;

    public Issue(int issueRate) {
        this.issueRate = issueRate;
    }

    public void issue(List<Instruction> issueList, List<Instruction> executeList) {
        List<Instruction> readyInstructions = new ArrayList<>();
        for (Instruction instr : issueList) {
            if (instr.isReady()) {
                readyInstructions.add(instr);
            }
        }

        readyInstructions.sort(Comparator.comparingInt(Instruction::getTag));

        int issuedCount = 0;
        // getting all ready instructions
        for (Instruction instr : readyInstructions) {
            if (issuedCount >= issueRate) {
                break;
            }
            // decrementing the scheduling queue entry
            issueList.remove(instr);
            // state from IS to EX
            instr.setState(InstructionState.EX);
            // setting execution timer based on operation type
            instr.setExecutionTimer(getLatency(instr.getOpType()));

            executeList.add(instr);

            issuedCount++;
        }
    }

    // returns latency based on operation type
    private int getLatency(int opType) {
        switch (opType) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 5;
            default:
                return 1;
        }
    }
}
