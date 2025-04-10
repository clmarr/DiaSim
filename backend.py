from flask import Flask, send_from_directory
from flask_cors import CORS
import os
import webbrowser
import threading
import time

app = Flask(__name__, static_folder='./ui/guima/build')
CORS(app)  # Enable CORS for all routes

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

def open_browser():
    """Open browser after a short delay"""
    time.sleep(1)
    webbrowser.open_new('http://localhost:5000/')

if __name__ == '__main__':
    # Start browser in a new thread
    threading.Thread(target=open_browser).start()
    
    # Start Flask server
    app.run(debug=True, use_reloader=False)
    
    print("Server running on http://localhost:5000/")