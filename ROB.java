import java.util.LinkedList;
import java.util.Queue;

public class ROB {
    private static final int SIZE = 1024;
    private Queue<Instruction> rob;

    public ROB() {
        rob = new LinkedList<Instruction>();
    }

    public boolean addInstruction(Instruction i) {
        if (rob.size() < SIZE) {
            rob.add(i);
            return true;
        }
        return false; // ROB is full (should not happen)
    }

    public Instruction removeInstruction() {
        if (!rob.isEmpty()) {
            if (rob.peek().getState() == Instruction.State.WB) {
                return rob.remove();
            }
        }
        return null; // ROB is empty or head is not in WB state
    }

    public void advancePipeline() {
        for (Instruction i: rob) {
            i.advanceState();
        }
    }

    public boolean isEmpty() {
        return rob.isEmpty();
    }
    
}