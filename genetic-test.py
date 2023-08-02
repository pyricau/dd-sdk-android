#  Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
#  This product includes software developed at Datadog (https://www.datadoghq.com/).
#  Copyright 2016-Present Datadog, Inc.

import subprocess
import random
import os

# Constants for the genetic algorithm
POPULATION_SIZE = 3
NUM_GENERATIONS = 3
MUTATION_RATE = 0.1

# Function to call the Gradle script and get the solution score
def evaluate_solution(input_values):
    print(f"Evaluating solution for: {input_values}")
    gradle_command = f" ./gradlew :sample:kotlin:runGeneticTest --uploadFrequencyRate={input_values[0]} --maxBatchSizeRate={input_values[1]} --maxItemSizeRate={input_values[2]} --recentDelayRate={input_values[3]}"
    gradle_process = subprocess.Popen(gradle_command.split(), stdout=subprocess.PIPE)
    _, _ = gradle_process.communicate()
    score_file_path = "sample/kotlin/score.txt"
    with open(score_file_path, "r") as file:
        score = float(file.read())
        print(f"Score was: {score}")
    os.remove(score_file_path)
    return score

# Function to create an initial population of chromosomes
def create_initial_population():
    population = []
    for _ in range(POPULATION_SIZE):
        chromosome = [random.uniform(0, 1) for _ in range(4)]  # Assuming input values are in the range [0, 1]
        population.append(chromosome)
    return population

# Function to perform selection using tournament selection
def tournament_selection(population, fitness_values):
    selected_population = []
    for _ in range(POPULATION_SIZE):
        tournament_size = min(5, len(population))
        tournament = random.sample(list(enumerate(population)), tournament_size)
        winner = max(tournament, key=lambda x: fitness_values[x[0]])
        selected_population.append(winner[1])
    return selected_population

# Function to perform crossover using single-point crossover
def crossover(parent1, parent2):
    crossover_point = random.randint(1, len(parent1) - 1)
    child1 = parent1[:crossover_point] + parent2[crossover_point:]
    child2 = parent2[:crossover_point] + parent1[crossover_point:]
    return child1, child2

# Function to perform mutation on a chromosome
def mutate(chromosome):
    for i in range(len(chromosome)):
        if random.random() < MUTATION_RATE:
            chromosome[i] = random.uniform(0, 1)
    return chromosome

# Main genetic algorithm
def genetic_algorithm():
    population = create_initial_population()

    for generation in range(NUM_GENERATIONS):
        fitness_values = [evaluate_solution(chromosome) for chromosome in population]

        # Perform elitism (retain the best solution from the previous generation)
        best_index = fitness_values.index(max(fitness_values))
        new_population = [population[best_index]]

        # Perform selection, crossover, and mutation to create the new population
        while len(new_population) < POPULATION_SIZE:
            parent1, parent2 = random.choices(population, weights=fitness_values, k=2)
            child1, child2 = crossover(parent1, parent2)
            child1 = mutate(child1)
            child2 = mutate(child2)
            new_population.extend([child1, child2])

        population = new_population

    best_solution = population[fitness_values.index(max(fitness_values))]
    return best_solution, max(fitness_values)

if __name__ == "__main__":
    best_solution, best_score = genetic_algorithm()
    print(f"Best Solution: {best_solution}")
    print(f"Best Score: {best_score}")
    print("Running best solution...")
    evaluate_solution(best_solution)
    with open("best_score.txt", "a") as file:
        # file.write(f"Best score: {best_score} for uploadFrequencyRate={best_solution[0]} maxBatchSizeRate={best_solution[1]} maxItemSizeRate={best_solution[2]} recentDelayRate={best_solution[3]}")
        file.write(f"Best score: {best_score} for uploadFrequencyRate={best_solution[0]} maxBatchSizeRate={best_solution[1]} maxItemSizeRate={best_solution[2]} recentDelayRate={best_solution[3]}")
        file.write("\n")
