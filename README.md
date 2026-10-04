# Inverted Index - Performance Benchmarking (Stage 1)

This repository contains Stage 1 of the Big Data project. The goal is to ingest e-books from Project Gutenberg, build an inverted index, and benchmark the performance (execution time, memory usage) across three different programming languages: Python, Java, and C#.

## 📁 Repository Structure

```text
📦 Inverted_Index
 ┣ 📂 data
 ┃ ┣ 📂 benchmarks       # Output folder for benchmark results (unified_metrics.csv)
 ┃ ┗ 📂 index            # Generated indexes (contains /java, /python, /csharp)
 ┣ 📂 source
 ┃ ┣ 📂 C#           # C# source code and benchmark runner
 ┃ ┣ 📂 java             # Java source code (Maven project)
 ┃ ┗ 📂 python           # Python source code (Ingestor and logic)
 ┣ 📜 .gitignore
 ┗ 📜 README.md
```
## ⚙️ Prerequisites

To run all components of this project, you need to have the following installed:
* **Python 3.8+** (with `pip` for dependency management)
* **Java JDK 17+** and **Maven**
* **.NET SDK 8.0+** (for C#)
* **Git**

## 🚀 Setup & Execution Instructions

### 1. Clone the repository
```bash
git clone https://github.com/Big-DataGroup/stage_1.git
cd stage_1
```

### 2. Python Execution
Executes the inverted index building and benchmarking process in Python
```bash
cd source/python
pip install requests
python main.py
```
### 3. Java Execution
Executes the inverted index building and benchmarking process in Java
```bash
cd source/java
mvn clean install
mvn exec:java -Dexec.mainClass="Main"
```

### 4. C# Execution
Executes the inverted index building and benchmarking process in C#
```bash
cd source/C#
dotnet run
```

## 📊 Benchmarking Results
Each language module generates its own performance metrics after completing the inverted index generation. These metrics are consolidated into a single file located at:
data/benchmarks/unified_metrics.csv

This file contains the comparative analysis of execution times and resource consumption across Python, Java, and C#.

## 👥 Contributors
* **Alejandro Morales Monroy**
* **Daniela Eridenia Martel Valido**
* **Laura Medina Arencibia**
* **Francisco Javier Guerra Redondo**