// for now the requesting method is to print a unique string and map that unique string in the python backend
// this may need to change hence the abstraction
public class GuiHandler {
    private static boolean active;

    public static void SetActive(boolean value) {
        active = value;
    }

    public static void LoadingRequested() {
        System.out.println("");
    }
    public static void RequestMainMenu() {
        System.out.println("MAIN MENU REQUESTED");
    }

    public static void RequestQueryMenu() {
        System.out.println("QUERY MENU REQUESTED");
    }
}