# Universal Algebra Calculator – Extended Version

This project is an extension of the original **Universal Algebra Calculator (UACalc)** developed by Ralph Freese and collaborators.

Original UACalc: http://www.uacalc.org/

UACalc is a well-known tool for computations in universal algebra, especially for working with finite algebras, congruences, subalgebras, products, and related algebraic structures. This repository builds on that foundation and extends it with additional features motivated mainly by practical use.

---

## Disclaimer

This project is a personal extension of UACalc created primarily for **practical experimentation, research-oriented use, and usability improvements**.

I am not a professional software engineer, and this repository should not be understood as a polished industrial-grade fork. The main goal is to make the program more useful for concrete work, even if some parts of the code are still rough, experimental, or not ideally structured.

So, in short:

- the focus is on practical functionality  
- some parts of the implementation may be imperfect  
- some features are still evolving  
- stability and architecture may improve gradually over time  

This project is provided in good faith, but without guarantees of completeness, stability, or full backwards compatibility.

---

## What Is Added Compared to the Original UACalc

This version extends the original UACalc in several directions.

### First-Order Logic Support

- parsing and representation of first-order formulas  
- support for variables and terms  
- integration within the relations framework  

### Relations Framework

- support for abstract relations defined independently of a concrete algebra  
- definition of relations by first-order formulas  
- management of relations within the UI  

### Relation Implementations

- interpretation of abstract relations on concrete algebras  
- computation of relation tuples  
- visualization and inspection of implemented relations  

### LaTeX and TikZ Export

- additional LaTeX export options  
- export of algebra drawings to TikZ  

### Other Changes

- various minor UI improvements  
- additional practical functionalities such as **renaming**, **filtering**, and similar workflow-oriented changes  

---

## Lua Dependency

LaTeX/TikZ export functionality in this project relies on **Lua-based code**.

Because of that, you may need to have **Lua installed on your system** and available from the command line for these features to work properly.

---

## Work in Progress / Known Limitations

This project is still under active development, and some parts are unfinished or experimental.

Current limitations may include:

- incomplete or evolving UI behavior  
- rough edges in parsing and error handling  
- changing internal structure  
- features that work for practical use but still need cleanup or better generalization  

---

## Possible Future Work

Planned or considered directions include:

- additional features for checking properties of relations  
- improved support for visualization and drawing of relations  
- support for working with first-order theories and axiomatizations  
- integration with automated theorem provers  
- development of a more efficient computation core (possibly in Rust)  
- further improvements to UI and overall usability  
- continued refactoring and stabilization of the codebase  

---

## Acknowledgements

Full credit for the original UACalc belongs to its original authors and maintainers. This project is only an extension built on top of their work.

---

## License

Please check the licensing conditions of the original UACalc project and use a compatible license for this repository if appropriate.