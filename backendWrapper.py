import pwn

class backendWrapper:
    io:pwn.process
    def __init__(self, args: list[str]):
        self.io = pwn.process(args)

    def next(self):
        return self.io.recvuntil(b"GUI ENCLOSER: ").decode()
    def get_type(self):
        return self.io.recvline()