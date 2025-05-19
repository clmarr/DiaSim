import java.util.Map;

public class GUIEncloser {
    enum DataType {
        String, number,
    }

    private static final Map<DataType, String> datatypeToString = Map.of(
        DataType.String, "string",
        DataType.number, "number"
    );
    public static void Text() {
        indicate("text");
    }
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
    private static void standardOption(String option, String ret_option, DataType type) {
        System.out.println("GUI OPTION: " + option + ", RETURN VALUE" + ret_option + ", RETURN TYPE" + datatypeToString.get(type));
    }
}