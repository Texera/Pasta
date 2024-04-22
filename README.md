## Pasta: A Cost-Based Optimizer for Generating Pipelining Schedules for Dataflow DAGs
### Source code for the Pasta Optimizer

#### Expected Input File:

- Workflow sources from other platforms need to be converted into a **.dot file** to use Pasta.
- Plase follow the file `src/WorkflowParser/sample_dotfile.dot` for expected format.

Explanation of the sample file:

```
digraph {
	"5400285081714527009_2" [label=GroupBy]           // <operator_1_id> <operator_1_type>
	"5400285081714527009_1" [label="CSV Reader"]      // <operator_2_id> <operator_2_type>
	"5400285081714527009_1" -> "5400285081714527009_2" [label="data: 340741; is_blocking: False"]
	// edge_1 expressed as: <operator_2_id> -> <operator_1_id>, labels annotate cost and blocking information.
	rankdir=LR
}

```

#### Steps to run experiments:

1. Clone and open this repo in Intellij IDEA. 
2. Open `src/RunWorkflowExperimentsOnSingleDotFile.java` and execute `RunWorkflowExperimentsOnSingleDotFile` with Intellij **with two CLI arguments** specifying input file and output directory, respectively.
3. An example CLI argument: `"<path_to_your_input_directory>/<input_file>.dot" "<path_to_your_output_directory>"`.
4. The input physical plan as well as the output execution plans of each method will be written to your specified directory.
5. Additionally, you will see a CSV file specifying the details of all the experiments on this file.
- The experiments for a single include 32 combinations of 5 different configs:
  - Top-down / Bottom-up Search
  - Exhaustive / Greedy Search
  - Optimization 1 (Early Stopping) disabled / enabled.
  - Optimization 2 (Chains) disabled / enabled.
  - Optimization 3 (Clean Edges) disabled / enabled.