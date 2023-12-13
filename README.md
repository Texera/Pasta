## Pasta: A Cost-Based Optimizer for Generating Pipelining Schedules for Dataflow DAGs
### Source code for the Pasta Optimizer

#### Steps to run experiments:

1. Clone and open this repo in Intellij IDEA.
2. To run experiments on synthetic physical plans:
   1. Open `src/RunRandomDAGExperiment`and execute `RunRandomDAGExperiment` with Intellij. You will see errors because the arguments have not been correctly set yet.
   2. Modify the output path to be your desired path (follow the TODO in this source file.)
   3. Modify the run configurations accordingly in Intellij with "Edit Configurations" by entering CLI arguments in the following format:
      `-numVertices <XX> -numEdges <XX> -seed <XX> -pBEdge <XX> -forceChain <true/false> -pForceChain <XX>`. The meaning of each argument:
      1. `numVertices`: The number of vertices in the generated physical plan DAG.
      2. `numEdges`: The number of edges in the generated physical plan DAG.
      3. `seed`: The seed for generating random costs for the edges.
      4. `pBEdge`: The probability (0-1) for an edge to be blocking in the physical plan.
      5. `forceChain`: Set this to `true` if you want the generated physical plan to have more and longer chains. Otherwise, the DAG is purely random.
      6. `pForceChain`: The probability (0-1) for an edge to be in a chain when generating edges in the physical plan. 
   4. An example CLI argument: `-numVertices 30 -numEdges 35 -seed 51 -pBEdge 0.3 -forceChain true -pForceChain 0.99`.
   5. After setting the arguments in the run configuration, execute the program and the input physical plan as well as the output execution plans of each method will be written to your specified directory.
3. To run experiments on real workflows (**NOTE this repo does NOT include the workflow source files for copyright reasons, so you need to gather them yourself**):
   1. Open `src/RunWorkflowExperiment` and execute `RunWorkflowExperiment` with Intellij. You will see errors because the arguments have not been correctly set yet.
   2. Modify the output path to be your desired path (follow the TODO in this source file.)
   3. Modify the run configurations accordingly in Intellij with "Edit Configurations" by entering CLI arguments in the following format:
      
      `<File name> <Seed (optional)>`, where `<File name>` is the path to either an Alteryx `.yxmd` file or a KNIME Workflow Summary `.xml` file.
   4. If the workflow source file already has cost information, e.g., A KNIME workflow summary file contains execution statistics which will be used to calculate real costs, leave `<cost>` blank.
   5. If the workflow source file does not contain cost information, e.g., An Alteryx YXMD file does not have execution details, you can manually enter the cost information based in the YXMD file by adding a tag `<Mat Size="XXX"/>` in each `<Connection></Connection>` field (i.e., the edges), where `XXX` is an Integer cost. 
   6. Alternatively you can choose to randomly generate the costs for each edge in the physical plan by specifying the seed as an integer. 
   7. An example CLI argument: `"/Users/xzliu/Desktop/Experiments/Alteryx Workflows/Samples/en/06 Diagnose_why_it_is_happening/04 Create_a_report_with_a_thematic_map.yxmd" 36`.
   8. After setting the arguments in the run configuration, execute the program and the input physical plan as well as the output execution plans of each method will be written to your specified directory.