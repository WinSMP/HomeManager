{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }: 
  let
    pkgs = nixpkgs.legacyPackages.x86_64-linux;
  in {
    devShells.x86_64-linux.default = pkgs.mkShell {
      buildInputs = [
        pkgs.jdk21_headless
        pkgs.gradle
        pkgs.git
      ];
      shellHook = ''
        echo "Welcome to the development environment of HomeManager!"
      '';
    };
  };
}
