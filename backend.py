# DO NOT IN ANY CIRCUMSTANCES USE THIS IN PRODUCTION!! IT DOES NOT HAVE SECURITY
from backendWrapper import backendWrapper
from flask import Flask, jsonify, send_from_directory
import os
import easygui
from flask import request


app = Flask(__name__, static_folder='./ui/guima/build')
io:backendWrapper

master_data= {}

# API routes
@app.route('/api/hello', methods=['GET'])
def hello():
    return {'message': 'Hello from Flask!'}

# Serve React app
@app.route('/', defaults={'path': ''})
@app.route('/<path:path>')
def serve(path):
    if path != "" and os.path.exists(app.static_folder + '/' + path):
        return send_from_directory(app.static_folder, path)
    else:
        return send_from_directory(app.static_folder, 'index.html')


@app.route("/filename")
def get_file_name():
    # Get the directory where the backend script is located
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    print(backend_dir)
    temp = {}
    temp["filename"] = easygui.fileopenbox(default=backend_dir)
    master_data["start_args"] = {type: temp["filename"]}
    return jsonify(temp)

@app.route("/start/config", methods=["POST"])
def start_json():
    global io
    data = request.get_json()
    # Store the configuration data in the master_data dictionary
    master_data["config"] = data
    print(data)
    # tempArgs = ["java -cp bin "DiachronicSimulator"]
    activeFlags = "-"
    # for key in data:
    #     if data[key] == "true":
    #         activeFlags += key[0]
    #     tempArgs.append(f"-{key} {data[key]}")
    # tempArgs.append(activeFlags)
    # print(tempArgs)
    # io = backendWrapper(
    # )
    
    
    return jsonify([{ "text": "option 0", "return": "0", "type": "button" },
    { "text": "option 1", "return": "1", "type": "button" }])

@app.route("/next")
def next_io():
    return

@app.route("/sendline", methods=["POST"])
def sendLine():
    global io
    data = request.get_json()
    io.io.sendline(data["return"])
    return jsonify({"status" : "200"})

@app.route("/api/data", methods=['GET'])
def get_data():
    # Get query parameters from the request
    args = request.args
    if args.get("start") == "true":
        start_args = [
            {"title": "lex", "type": "file", "hint": "Sets the file with the etyma to implement sound changes on"},
            {"title": "rules", "type": "file", "hint": "Sets the file with the ordered sound changes to realize upon the lexicon"},
            {"title": "out", "type": "string", "hint": "Name for the output folder with all resulting forward-reconstructions and analysis files"},
            {"title": "symbols", "type": "file", "hint": "Symbol definitions file (optional, default: symbolDefs.csv)"},
            {"title": "impl", "type": "file", "hint": "Feature implications file (optional, default: FeatureImplications)"},
            {"title": "diacrit", "type": "file", "hint": "Custom diacritics file (optional)"},
            {"title": "idcost", "type": "int", "hint": "Cost of insertion and deletion for computing edit distances"},
            {"title": "verbose", "type": "bool", "hint": "Prints more information about file locations and other variables"},
            {"title": "print", "type": "bool", "hint": "Print changes mode - prints words changed by each rule"},
            {"title": "halt", "type": "bool", "hint": "Halt mode - halts at all intermediate stages"},
            {"title": "explict", "type": "bool", "hint": "Explicit mode - ignores feature implications"},
            {"title": "skip", "type": "bool", "hint": "Skip file creation - runs without creating output folder"}
        ]
        return jsonify(start_args)
    elif args.get("start_percentages") == "true":
        return jsonify("nothing")
    # Your function code here
    return jsonify({"data": "This is the data endpoint"})

# def open_browser():
#     """Open browser after a short delay"""
#     time.sleep(1)
#     webbrowser.open_new('http://localhost:5000/')

if __name__ == '__main__':
    # Start browser in a new thread
    # threading.Thread(target=open_browser).start()
    
    # Start Flask server
    app.run(debug=True, use_reloader=False)
    
    print("Server running on http://localhost:5000/")