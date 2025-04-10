public class GUIEncloser {    
    public static void Menu() {
        indicate("main menu");
    }
    public static void singleInput() {
        indicate("single input");
    }
    public static void menuInput() {
        indicate("menu input");
    }
    private static void indicate(String option) {
        System.out.println("\nGUI ENCLOSER: " + option);
    }
}