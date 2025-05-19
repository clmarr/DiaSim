{pkgs ? import <nixpkgs> {}}:
  
let
  pythonEnv = pkgs.python3.withPackages (ps: with ps; [
    pwntools
    tkinter
    pip
    flask
    easygui
    # Add any other packages you need here
  ]);
in
  (pkgs.buildFHSUserEnv {
    name = "ctf";
    targetPkgs = pkgs: (with pkgs; [ 
      pythonEnv
      gcc
      llvmPackages_latest.clang
      clang-tools
      wireshark
      strace
      pwntools
      pwndbg
      zulu17
      unzip
      steam-run
      wget
      sqlmap
      exiftool
      tmux
    ]);
    runScript = ''zsh
    mkdir -p .nix-wrappers
    ln -sf $(which pwndbg) .nix-wrappers/pwntools-gdb
    export PATH=$PWD/.nix-wrappers:$PATH
    '';
}).env
