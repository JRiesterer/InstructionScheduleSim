import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Simulator {
    private Queue<Instruction> dispatch_list;
    private PriorityQueue<Instruction> issue_list;
    private List<Instruction> execute_list;
    private List<Instruction> fakeROB;
    private Map<Integer, Integer> registerMap;
    private int N;
    private int queueSize;
    private int cycle = 0;

    private Queue<Instruction> instructionBuffer; // Holds read instructions from file

    public Simulator(int N, int queueSize, String traceFile) throws IOException {
        this.N = N;
        this.queueSize = queueSize;
        this.dispatch_list = new ArrayDeque<>(2*N);
        this.issue_list = new PriorityQueue<>(Comparator.comparingInt(Instruction::getTag));
        this.execute_list = new ArrayList<>();
        this.fakeROB = new ArrayList<>();
        this.registerMap = new HashMap<>();
        this.instructionBuffer = new LinkedList<>();

        loadInstructions(traceFile);
    }

    private void loadInstructions(String traceFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(traceFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length != 5) continue; // Skip malformed lines
                
                String PC = parts[0];
                int optype = Integer.parseInt(parts[1]);
                int dest = Integer.parseInt(parts[2]);
                int src1 = Integer.parseInt(parts[3]);
                int src2 = Integer.parseInt(parts[4]);

                Instruction inst = new Instruction(PC, optype, dest, src1, src2);
                instructionBuffer.add(inst);
            }
        }
    }



    public void run() {
        while (!fakeROB.isEmpty() || !instructionBuffer.isEmpty()) {
            cycle++;
            System.out.println("Cycle " + cycle);
            FakeRetire();
            Execute();
            Issue();
            Dispatch();
            Fetch();
        }
    }

    private void FakeRetire() {
       while (!fakeROB.isEmpty() && fakeROB.get(0).getState() == Instruction.State.WB) {
            fakeROB.remove(0);
           //Instruction i = fakeROB.remove(0);
           //registerMap.put(i.getDest(), i.getTag());
       }
    }

    private void Execute() {
        Iterator<Instruction> it = execute_list.iterator();
        while (it.hasNext()) {
            Instruction i = it.next();
            i.decrementCycle();
            if (i.isExecutionComplete()) {
                i.setState(Instruction.State.WB);
                registerMap.put(i.getDest(), null);
                wakeUpInstructions(i.getDest());
                it.remove();
            }
        }
    }

    private void wakeUpInstructions(int dest) {
        for (Instruction i: issue_list) {
            if (i.getSrc1() == dest) {
                i.setSrc1Ready();
            }
            if (i.getSrc2() == dest) {
                i.setSrc2Ready();
            }
        }
    }

    private void Issue() {
        List<Instruction> toRemove = new ArrayList<>();
        int issueCount = 0;

        for (Instruction i: issue_list) {
            if (issueCount >= N + 1) break;
            if (i.isReady()) {
                i.setState(Instruction.State.EX);
                execute_list.add(i);
                toRemove.add(i);
                issueCount++;
            }
        }
        issue_list.removeAll(toRemove);
    }

    private void Dispatch() {
        List<Instruction> toRemove = new ArrayList<>();
        int dispatchCount = 0;

        for (Instruction i: dispatch_list) {
            if (dispatchCount >= N || issue_list.size() >= queueSize) break;
            if (i.getState() == Instruction.State.ID) {
                i.setState(Instruction.State.IS);
                issue_list.add(i);
                // RENAME REGISTERS
                // #TODO: Check this, IDK what it's doing
                renameRegisters(i);
                toRemove.add(i);
                dispatchCount++;
            }
        }
        dispatch_list.removeAll(toRemove);

        for (Instruction i: dispatch_list) {
            if (i.getState() == Instruction.State.IF) {
                i.advanceState();;
            }
        }
    }

    private void renameRegisters(Instruction i) {
        if (registerMap.containsKey(i.getSrc1())) i.setSrc1Ready();
        if (registerMap.containsKey(i.getSrc2())) i.setSrc2Ready();
        registerMap.put(i.getDest(), i.getTag());
    }

    private void Fetch() {
        for (int i=0; i < N; i++) {
            if (dispatch_list.size() >= 2*N) break;
            Instruction inst = instructionBuffer.poll();
            if (inst != null) {
                inst.setState(Instruction.State.IF);
                fakeROB.add(inst);
                dispatch_list.add(inst);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java Simulator <schedulingQueueSize> <nWay> <traceFile>");
            return;
        }

        try {
            int nWay = Integer.parseInt(args[1]);
            int schedulingQueueSize = Integer.parseInt(args[0]);
            String traceFile = args[2];

            Simulator simulator = new Simulator(nWay, schedulingQueueSize, traceFile);
            simulator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
