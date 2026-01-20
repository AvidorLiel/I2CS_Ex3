# 2D Map & Pac-Man Algorithms – Ex3

**Author:** Liel Avidor  
**Course:** Introduction to Computer Science  
**Assignment:** Exercise 3 – 2D Algorithms & Strategy Logic

---

## Overview

This project implements an advanced logic engine for a 2D grid environment, focusing on **Breadth-First Search (BFS)** for pathfinding, automated decision-making, and geometric processing. 

The primary goal of the implementation is to control an agent (Pac-Man) using a deterministic algorithm that balances **efficiency** (collecting food), **survival** (avoiding ghosts), and **stability** (avoiding oscillations).

---

## The Intelligence Engine: `Ex3Algo.java`

The `Ex3Algo` class is the "brain" of the project. It handles the core logic of how the agent interacts with the world, manages ghost threats, and plans its route.

### 1. Decision Hierarchy & Strategy
The algorithm evaluates the game state at every turn and operates according to the following priorities:

* **Survival Priority (Escape Mode):** If a ghost is detected within a dangerous range, the algorithm immediately calculates an escape route. It uses `allDistance` to find a move that maximizes the distance from the nearest threat.
* **Green Mode (Power-Up Aggression):** When ghosts are in an "eatable" state, the algorithm switches to an aggressive mode. It locks onto the nearest ghost and treats it as the primary target using a shortest-path BFS.
* **Efficiency Mode (Food Gathering):** When safe, the algorithm identifies the closest pink target (food) and follows the optimal path calculated via the `shortestPath` function.

### 2. Ghost Interaction & Risk Assessment
* **Safety Buffers:** The algorithm doesn't just avoid the ghost's current pixel; it evaluates the "pressure" of the area to avoid getting trapped in dead-ends.
* **Deterministic Locking:** Once a target is selected (either food or an eatable ghost), the agent stays committed to that target until it is consumed, preventing "target switching" jitter.

---

## Core Algorithmic Functions

### Pathfinding & Distances
* **`shortestPath(...)`** Computes the optimal sequence of pixels between two points while avoiding obstacles of a specific color.
* **`allDistance(...)`** Generates a full distance map from a source point to all reachable cells. This is the foundation for all movement decisions.

### Map Manipulation
* **`fill(...)`** A logic-based area fill (flood fill) that identifies and colors a continuous area bounded by a specific color.
* **Geometric Shapes** Implementation of `drawLine`, `drawRect`, and `drawCircle` for grid-based rendering.

---

## Anti-Oscillation & Stability

To prevent "dancing" (moving back and forth between two points), the algorithm implements:
1. **Directional Penalty:** Returning to the previous pixel is heavily penalized unless it's the only way to escape a ghost.
2. **Path Locking:** The algorithm maintains its intended path for several frames to ensure smooth and natural movement.



---

## Memory & State Management

The algorithm is designed to be **stateless-resilient**. It maintains internal static memory to keep track of its previous position and current mode. If a "teleport" or "respawn" is detected (the distance between current and previous position is too large), the internal memory resets to ensure clean decision-making for the new state.

---

## How to Run

1. **Compile the project:**
   ```bash
   javac *.java


Use of AI Assistance
AI tools were utilized during development for:

Optimizing BFS performance for large maps.

Structuring documentation and improving code readability.

Debugging coordinate inversion between the grid and the GUI.

<img width="1074" height="1025" alt="image" src="https://github.com/user-attachments/assets/53637fee-ab91-445b-a493-e80637e3d685" />
