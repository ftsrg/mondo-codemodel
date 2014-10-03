package hu.bme.mit.codemodel.jamoppdiscoverer;

public class Main {

    public static void main(String[] args) {
        CommandParser commandParser = new CommandParser();

        commandParser.initOptions();
        commandParser.processArguments(args);
        commandParser.execute();
    }

}
