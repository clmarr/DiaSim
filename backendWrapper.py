import pwn

class backendWrapper:
    # Map of menu types to handler functions
    type_map = {
        "main menu": "handle_main_menu",
        "text": "handle_text",
        "info": "handle_info",
        "dialog": "handle_dialog",
        "input": "handle_input",
        "number": "handle_number"
    }
    io: pwn.process

    def handle_response(self, response_type, data):
        handler_name = self.type_map.get(response_type)
        if handler_name:
            handler = getattr(self, handler_name, None)
            if handler and callable(handler):
                return handler(data)
        return None
    def __init__(self, args: list[str]):
        self.io = pwn.process(args)
    def get_next(self):
        ret_data = {}
        temp = self.next()
        key = self.get_type()
        ret_data[key] = self.handle_response(key, temp)
        return ret_data
    def next(self):
        return self.io.recvuntil(b"GUI ENCLOSER: ").decode()
    def get_type(self):
        return self.io.recvline().decode().strip()
    def handle_main_menu(self, temp):
        assert(type(temp) == str)
        ret_data = []
        for i in temp.split('\n'):
            temp_ret_data = {}
            if ":" in i:
                i = i.replace("|", "")
                temp_ret_data["type"] = "button"
                if "#" in i:
                    temp_ret_data["type"] = "number"
                k, v = i.split(':', 1)
                temp_ret_data["text"] = v.strip()
                temp_ret_data["return"] = k.strip()
                ret_data.append(temp_ret_data)
        return ret_data