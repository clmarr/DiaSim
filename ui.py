import os
import json
import tkinter as tk
from tkinter import Button, filedialog
import argparse
from turtle import clear
import pwn
io = pwn.process
root = tk.Tk
# percent bar to show accuracy metrics
# loading bar
# browse files
# add additional files (change file path to browse)
# ACCURACY REPORT:
# Overall accuracy:                        0.85268505
# Accuracy within 1 phone:                 0.94847605
# Accuracy within 2 phones:                0.99129173
# Average edit distance from gold:         0.05968104
# Average feature edit distance from gold: 0.05544546
# future feature diachronis thing change cascade from java program
# potential 
menu_start = ["Available options for pivot point:","~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~# SUITE MENU #~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~",  "What is your query? Enter the corresponding indicator:", "What results would you like to check? Please enter the appropriate number:", "Please indicate which metric for correlation to error you would like to use:", "What would you like to do?" ]
menu_bookends = ["|_9 : End this analysis.______________________________________________________________|","Please enter the appropriate indicator.", "9 : return to main menu.", "| 9 : Exit this menu._________________________________________________________________|", "        'comp': comparison of prepared values for the four different metrics.", "9: Cancel entire forking test and return to main menu.", ]

input_indicators = ["Otherwise, enter the phoneme sequence filter, delimiting phones with", "Enter the ID to query:","                        'get lexicon', print entire lexicon with etyma mapped to inds.", "Please enter the indicator for the stage you desire"]

def diasim_main_screen(args, dynamic_frame):
    global io
    command = ["java", "-cp", "bin", "DiachronicSimulator"] + args
    io = pwn.process(command)
    io.recvuntil(b'done.\n')
    output = io.recvuntil(b'9 : ').decode() + io.recvline().decode()
    handle_main_menu(io, output, dynamic_frame)
    
    
def recursive_frame_make(io:pwn.process, menu_info,dynamic_frame, print_data):
    global frame_stack
    # print("recursive_frame_make, print_data:", print_data)
    text_widget = tk.Text(dynamic_frame)
    # dynamic_frame.bind("", lambda event: on_input(output, text_widget))
    text_widget.insert(tk.END, print_data)
    text_widget.pack(side=tk.LEFT, fill="both", expand=True)
    temp_frame = tk.Frame(dynamic_frame)
    for line in menu_info.splitlines():
        if ":" in line and line.find(":") > len(line) / 4:
            # print(line)
            assert type(line) == str
            text_widget.insert(tk.END, line + "\n")
        if ":" in line and line.find(":") < len(line) / 4:
            # TODO Implement a select your own that calls the on_input method for extensibilty
            # print(line)
            assert type(line) == str
            button_text = line.replace('_', "").replace('|', "").split(":", 1)
            create_dynamic_button(button_text[1].strip(), button_text[0].strip(), io, temp_frame, dynamic_frame)
    
    temp_frame.pack(side=tk.RIGHT, fill="both", expand=True)
    dynamic_frame.pack(fill="both", expand=True)
    

def create_dynamic_button(text, option, io, temp_frame, dynamic_frame):
    # TODO: implenment some sort of selection method in the buttons (e.g. R#)
    # if option == "R#":
    #     create_input_button(text, option, io, temp_frame, dynamic_frame)
    # else:
        button = tk.Button(temp_frame, text=f"{option}:{text}", command=lambda: button_action(option, io, dynamic_frame))
        button.pack(side=tk.TOP, fill="both", expand=True)
        
def create_input_button(text, option, io, temp_frame, dynamic_frame):
    def on_button_click():
        input_window = tk.Toplevel(root)
        input_window.title("Input Required")
        tk.Label(input_window, text="Enter input:").pack()
        input_entry = tk.Entry(input_window)
        input_entry.pack()
        tk.Button(input_window, text="Submit", command=lambda: submit_input(input_entry, input_window)).pack()

    def submit_input(input_entry, input_window):
        input_text = input_entry.get()
        button.config(text=f"{option}:{text} ({input_text})")
        input_window.destroy()

    button = tk.Button(temp_frame, text=f"{option}:{text}", command=on_button_click)
    button.pack(side=tk.TOP, fill="both", expand=True)
def clear_frame(frame):
    """
    selfDefined function that
    Clears all widgets from the given frame.
    Args:
        frame (tk.Frame): The frame from which all child widgets will be removed.
    Returns:
        None
    """
    for widget in frame.winfo_children():
        widget.destroy()
        
        
def button_action(option, io: pwn.process, dynamic_frame):
    assert type(option) == str
    io.sendline(option.encode())
    # print("Sending: ", option)
    clear_frame(dynamic_frame)
    parse_all_menus(io, dynamic_frame)


def parse_all_menus(io:pwn.process, dynamic_frame):
    global menu_start
    line_info = recv_till_many(io, menu_start + input_indicators).decode()
    is_menu = False
    submit_line_info = io.recvline().decode()
    for bookends in menu_bookends:
        if bookends in submit_line_info:
            submit_line_info = submit_line_info.replace(bookends, "")
    print("next line", submit_line_info)
    recieved = recv_till_many(io, menu_bookends).decode()
    print("parse_all_menus, recieved:", recieved)
    
    for start in menu_start:
        if start in recieved:
            is_menu = True
            if "SUITE MENU" in start:
                recieved.replace(start, "SUITE MENU")
            else:
                recieved.replace(start, "")
            break
    for end in menu_bookends:
        if end in recieved:
            is_menu = True
            recieved.replace(end, "")
            break
    # recieved = recieved.replace(line_info, "")
    if "SUITE MENU" in recieved:
        recieved = line_info + recieved
        handle_main_menu(io,recieved, dynamic_frame)
    elif is_menu:
        print("is_menu")
        recursive_frame_make(io, recieved, dynamic_frame, line_info)    
    else:
        # recieved = line_info + recieved
        print("is not menu")
        submenu_get_input(io, recieved + submit_line_info, dynamic_frame)
    
def print_info_only(io:pwn.process, output, dynamic_frame):
    text_widget = tk.Text(dynamic_frame)
    text_widget.insert(tk.END, output)
    text_widget.pack(side=tk.LEFT, fill="both", expand=True)
    tk.Button(dynamic_frame, text="Back", command=lambda: (clear_frame(dynamic_frame), parse_all_menus(io, dynamic_frame))).pack()
    dynamic_frame.pack(fill="both", expand=True)

def handle_main_menu(io:pwn.process, output, dynamic_frame):
    lines = output.splitlines()
    start_menu = False
    in_info = ""
    out_info = ""
    for line in lines:
        if "SUITE MENU " in line:
            start_menu = True
            continue
        if "-----" in line or ":" not in line:
            continue
        if start_menu and line != "":
            out_info += line + '\n'
        else:
            in_info += line + "\n"
    print(in_info)
    recursive_frame_make(io, out_info, dynamic_frame, in_info)
    
def submenu_get_input(io:pwn.process, recieved, dynamic_frame):
    ipa_symbols = ["ɑ", "æ", "ɔ", "ə", "ɛ", "ɪ", "ɒ", "ʊ", "ʌ", "ʃ", "ʒ", "θ", "ð", "ŋ"]

    def insert_ipa_symbol(symbol):
        text_input.insert(tk.END, symbol)

    ipa_frame = tk.Frame(dynamic_frame)
    ipa_frame.pack(side=tk.TOP, fill="x")

    for symbol in ipa_symbols:
        button = tk.Button(ipa_frame, text=symbol, command=lambda s=symbol: insert_ipa_symbol(s))
        button.pack(side=tk.LEFT)
    print("submenu_get_input, recieved:", recieved)
    text_widget = tk.Text(dynamic_frame)
    text_widget.insert(tk.END, recieved)
    text_widget.pack(side=tk.LEFT, fill="both", expand=True)
    text_input = tk.Entry(dynamic_frame)
    text_input.pack()

    tk.Button(dynamic_frame, text="Submit", command=lambda: on_input(io, text_input, text_widget, dynamic_frame)).pack()
    tk.Button(dynamic_frame, text="Back", command=lambda: (clear_frame(dynamic_frame), parse_all_menus(io, dynamic_frame))).pack()
    dynamic_frame.pack(fill="both", expand=True)


def recv_till_many(io:pwn.process, delimiter: list[str])-> bytes:
    data = b""
    while io.can_recv():
        chunk = io.recvline()
        data += chunk
        for delim in delimiter:
            if delim.encode() in chunk:
                io.unrecv(chunk)
                return data
    return data

def recv_till_line_before(conn:pwn.process, delimiter)-> bytes:
    data = b""
    while True:
        chunk = conn.recvline()
        data += chunk
        if delimiter.encode() in chunk:
            conn.unrecv(chunk)
            break
        print(chunk.decode(), end="")
    return data

def on_input(io:pwn.process, text_input, text_widget, dynamic_frame):
    global menu_start, menu_bookends
    assert type(text_input) == tk.Entry
    assert type(text_widget) == tk.Text
    print("action taken")
    print("inputted text: ", text_input.get())
    if len(text_input.get().strip()) > 0: 
        print("sending", text_input.get().strip())
        io.sendline(text_input.get().strip().encode())
        # recieved = io.recvuntil(menu_start[1].encode()).decode()
        # io.unrecv(menu_start[1].encode())
        recieved = recv_till_many(io, menu_start).decode()
        recieved = "\n".join(recieved.splitlines()[:-1])
        if recieved == "":
            recieved = io.recv().decode()
        io.unrecv(recieved.encode())
        # io.unrecv(get_menu(recieved.encode()))
        
        # print("in_recv: ", b"\n\n" + recieved.rsplit(b"\n\n", 1)[1])
        print("on_input recieved: ", recieved)
        text_widget.insert(tk.END, recieved)
        dynamic_frame.pack()
        # clear_frame(dynamic_frame)
        # handle_main_menu(io, recieved, dynamic_frame)

def get_menu(recieved:bytes) -> bytes:
    return recieved.rsplit(b"\n\n", 1)[1]
def save_options(options):
    with open("ui_options.json", "w") as f:
        json.dump(options, f)

def load_options():
    if os.path.exists("ui_options.json"):
        with open("ui_options.json", "r") as f:
            return json.load(f)
    return {}

def cli_main():
    parser = argparse.ArgumentParser(description="Run DiaSim from the command line.")
    parser.add_argument("-lex", type=str, help="Lexicon file")
    parser.add_argument("-rules", type=str, help="Cascade file")
    parser.add_argument("-out", type=str, help="Output folder")
    parser.add_argument("-symbols", type=str, help="Symbol definitions file")
    parser.add_argument("-impl", type=str, help="Feature implications file")
    parser.add_argument("-diacrit", type=str, help="Diacritics file")
    parser.add_argument("-idcost", type=float, help="Cost of insertion and deletion")
    parser.add_argument("-verbose", action="store_true", help="Verbose mode")
    parser.add_argument("-p", action="store_true", help="Print changes mode")
    parser.add_argument("-h", action="store_true", help="Halt mode")
    parser.add_argument("-e", action="store_true", help="Explicit mode")
    parser.add_argument("-s", action="store_true", help="Skip file creation")

    args = parser.parse_args()
    java_args = []
    if args.lex:
        java_args.extend(["-lex", args.lex])
    if args.rules:
        java_args.extend(["-rules", args.rules])
    if args.out:
        java_args.extend(["-out", args.out])
    if args.symbols:
        java_args.extend(["-symbols", args.symbols])
    if args.impl:
        java_args.extend(["-impl", args.impl])
    if args.diacrit:
        java_args.extend(["-diacrit", args.diacrit])
    if args.idcost:
        java_args.extend(["-idcost", str(args.idcost)])
    if args.verbose:
        java_args.append("-verbose")
    if args.p:
        java_args.append("-p")
    if args.h:
        java_args.append("-h")
    if args.e:
        java_args.append("-e")
    if args.s:
        java_args.append("-s")

    diasim_main_screen(java_args)

def gui_main():
    global frame_stack, root
    def select_folder(entry):
            file_path = filedialog.askdirectory()
            if file_path:
                entry.delete(0, tk.END)
                entry.insert(0, file_path)
    def select_file(entry):
        file_path = filedialog.askopenfilename()
        if file_path:
            entry.delete(0, tk.END)
            entry.insert(0, file_path)

    def run_simulation():
        options = {
            "lex": lex_entry.get(),
            "rules": rules_entry.get(),
            "out": out_entry.get(),
            "symbols": symbols_entry.get(),
            "impl": impl_entry.get(),
            "diacrit": diacrit_entry.get(),
            "idcost": idcost_entry.get(),
            "verbose": verbose_var.get(),
            "print_changes": print_changes_var.get(),
            "halt": halt_var.get(),
            "explicit": explicit_var.get(),
            "skip": skip_var.get()
        }
        save_options(options)

        args = []
        if options["lex"]:
            args.extend(["-lex", options["lex"]])
        if options["rules"]:
            args.extend(["-rules", options["rules"]])
        if options["out"]:
            args.extend(["-out", options["out"]])
        if options["symbols"]:
            args.extend(["-symbols", options["symbols"]])
        if options["impl"]:
            args.extend(["-impl", options["impl"]])
        if options["diacrit"]:
            args.extend(["-diacrit", options["diacrit"]])
        if options["idcost"]:
            args.extend(["-idcost", options["idcost"]])
        if options["verbose"]:
            args.append("-verbose")
        if options["print_changes"]:
            args.append("-p")
        if options["halt"]:
            args.append("-h")
        if options["explicit"]:
            args.append("-e")
        if options["skip"]:
            args.append("-s")

        show_dynamic_view(args)

    def show_dynamic_view(args):
        clear_frame(input_frame)
        dynamic_frame = tk.Frame(root)
        Button(input_frame, text="Abort", command=lambda: (root.destroy(), io.interactive())).pack()
        diasim_main_screen(args, dynamic_frame)
        input_frame.pack()
        dynamic_frame.pack()


    root = tk.Tk()
    root.title("DiaSim GUI")

    frame_stack = []

    input_frame = tk.Frame(root)
    input_frame.pack(fill="both", expand=True)

    options = load_options()

    tk.Label(input_frame, text="Lexicon File:").grid(row=0, column=0, sticky=tk.W)
    lex_entry = tk.Entry(input_frame, width=50)
    lex_entry.insert(0, options.get("lex", ""))
    lex_entry.grid(row=0, column=1)
    tk.Button(input_frame, text="Browse", command=lambda: select_file(lex_entry)).grid(row=0, column=2)
    ToolTip(lex_entry, "Select the lexicon file containing the etyma to process.")

    tk.Label(input_frame, text="Cascade File:").grid(row=1, column=0, sticky=tk.W)
    rules_entry = tk.Entry(input_frame, width=50)
    rules_entry.insert(0, options.get("rules", ""))
    rules_entry.grid(row=1, column=1)
    tk.Button(input_frame, text="Browse", command=lambda: select_file(rules_entry)).grid(row=1, column=2)
    ToolTip(rules_entry, "Select the cascade file containing the ordered sound changes.")

    tk.Label(input_frame, text="Output Folder:").grid(row=2, column=0, sticky=tk.W)
    out_entry = tk.Entry(input_frame, width=50)
    out_entry.insert(0, options.get("out", ""))
    out_entry.grid(row=2, column=1)
    tk.Button(input_frame, text="Browse", command=lambda: select_folder(out_entry)).grid(row=2, column=2)
    ToolTip(out_entry, "Select the folder where the output will be saved.")

    tk.Label(input_frame, text="Symbol Definitions File:").grid(row=3, column=0, sticky=tk.W)
    symbols_entry = tk.Entry(input_frame, width=50)
    symbols_entry.insert(0, options.get("symbols", ""))
    symbols_entry.grid(row=3, column=1)
    tk.Button(input_frame, text="Browse", command=lambda: select_file(symbols_entry)).grid(row=3, column=2)
    ToolTip(symbols_entry, "Select the file containing symbol definitions.")

    tk.Label(input_frame, text="Feature Implications File:").grid(row=4, column=0, sticky=tk.W)
    impl_entry = tk.Entry(input_frame, width=50)
    impl_entry.insert(0, options.get("impl", ""))
    impl_entry.grid(row=4, column=1)
    tk.Button(input_frame, text="Browse", command=lambda: select_file(impl_entry)).grid(row=4, column=2)
    ToolTip(impl_entry, "Select the file containing feature implications.")

    tk.Label(input_frame, text="Diacritics File:").grid(row=5, column=0, sticky=tk.W)
    diacrit_entry = tk.Entry(input_frame, width=50)
    diacrit_entry.insert(0, options.get("diacrit", ""))
    diacrit_entry.grid(row=5, column=1)
    tk.Button(input_frame, text="Browse", command=lambda: select_file(diacrit_entry)).grid(row=5, column=2)
    ToolTip(diacrit_entry, "Select the file containing diacritics definitions.")

    tk.Label(input_frame, text="ID Cost:").grid(row=6, column=0, sticky=tk.W)
    idcost_entry = tk.Entry(input_frame, width=50)
    idcost_entry.insert(0, options.get("idcost", ""))
    idcost_entry.grid(row=6, column=1)
    ToolTip(idcost_entry, "Set the cost of insertion and deletion for computing edit distances.")

    verbose_var = tk.BooleanVar(value=options.get("verbose", False))
    verbose_check = tk.Checkbutton(input_frame, text="Verbose", variable=verbose_var)
    verbose_check.grid(row=7, column=0, sticky=tk.W)
    ToolTip(verbose_check, "Enable verbose mode to print more information.")

    print_changes_var = tk.BooleanVar(value=options.get("print_changes", False))
    print_changes_check = tk.Checkbutton(input_frame, text="Print Changes", variable=print_changes_var)
    print_changes_check.grid(row=7, column=1, sticky=tk.W)
    ToolTip(print_changes_check, "Print words changed by each rule to the console.")

    halt_var = tk.BooleanVar(value=options.get("halt", False))
    halt_check = tk.Checkbutton(input_frame, text="Halt", variable=halt_var)
    halt_check.grid(row=7, column=2, sticky=tk.W)
    ToolTip(halt_check, "Halt at all intermediate stages.")

    explicit_var = tk.BooleanVar(value=options.get("explicit", False))
    explicit_check = tk.Checkbutton(input_frame, text="Explicit", variable=explicit_var)
    explicit_check.grid(row=8, column=0, sticky=tk.W)
    ToolTip(explicit_check, "Ignore feature implications.")

    skip_var = tk.BooleanVar(value=options.get("skip", False))
    skip_check = tk.Checkbutton(input_frame, text="Skip File Creation", variable=skip_var)
    skip_check.grid(row=8, column=1, sticky=tk.W)
    ToolTip(skip_check, "Run without creating a run output folder.")

    tk.Button(input_frame, text="Run Simulation", command=run_simulation).grid(row=9, column=0, columnspan=3)

    
    # tk.Button(dynamic_frame, text="Back", command=show_previous_view).pack()

    root.mainloop()

class ToolTip:
    def __init__(self, widget, text):
        self.widget = widget
        self.text = text
        self.tooltip = None
        self.widget.bind("<Enter>", self.show_tooltip)
        self.widget.bind("<Leave>", self.hide_tooltip)

    def show_tooltip(self, event):
        if self.tooltip or not self.text:
            return
        x, y, _, _ = self.widget.bbox("insert")
        x += self.widget.winfo_rootx() + 25
        y += self.widget.winfo_rooty() + 25
        self.tooltip = tk.Toplevel(self.widget)
        self.tooltip.wm_overrideredirect(True)
        self.tooltip.wm_geometry(f"+{x}+{y}")
        label = tk.Label(self.tooltip, text=self.text, background="yellow", relief="solid", borderwidth=1)
        label.pack()

    def hide_tooltip(self, event):
        if self.tooltip:
            self.tooltip.destroy()
            self.tooltip = None


if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        cli_main()
    else:
        gui_main()


