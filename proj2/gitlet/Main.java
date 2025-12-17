package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if(args.length ==0){
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command
                Repository.init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                Repository.add(args[1]);
                break;
            // TODO: FILL THE REST IN
            case "commit":
                Repository.commit(args[1]);
                break;
            case "log":
                Repository.log();
                break;
            case "find":
                Repository.find(args[1]);
                break;
            case "status":
                Repository.status();
                break;
            case "checkout":
                if(args.length ==2 ){
                    Repository.checkoutBranch(args[1]);
                } else if (args.length == 3 && args[1].equals("--") ) {
                    Repository.checkoutFile(args[2]);
                }else if (args.length == 4 && args[2].equals("--")){
                    Repository.checkoutFileFromCommit(args[1],args[3]);
                }  else{
                    System.out.println("Incorrect operands");
                    System.exit(0);
                }
                break;
            case "rm":
                if(args.length!=2){
                    System.out.println("Incorrect operands");
                    System.exit(0);
                }
                Repository.rm(args[1]);
                break;
            case "branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                Repository.branch(args[1]);
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "reset":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                Repository.reset(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

}

