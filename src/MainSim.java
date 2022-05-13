import jade.Boot;

public class MainSim {
    public static void main(String[] args){
        String[] param = new String[2];
        param[0] = "-gui";
        param[1] = " specialist:agent.SimSpecialistAgent;supplier:agent.SimSupplierAgent;";
        //param[2] = String.valueOf(customerStartText);
        Boot.main(param);
    }
}
