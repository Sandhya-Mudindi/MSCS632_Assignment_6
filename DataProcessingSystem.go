package main

import (
	"fmt"
	"log"
	"os"
	"sync"
	"time"
)

type TaskQueue struct {
	tasks chan string
}

func NewTaskQueue(size int) *TaskQueue {
	return &TaskQueue{
		tasks: make(chan string, size),
	}
}

func (q *TaskQueue) AddTask(task string) {
	q.tasks <- task
}

func (q *TaskQueue) GetTask() (string, bool) {
	task, ok := <-q.tasks
	return task, ok
}

func worker(name string, queue *TaskQueue, wg *sync.WaitGroup, mu *sync.Mutex, file *os.File) {
	defer wg.Done()
	log.Printf("%s started.\n", name)

	for {
		task, ok := queue.GetTask()
		if !ok {
			break
		}

		log.Printf("%s started processing %s.\n", name, task)

		// Simulate processing
		time.Sleep(500 * time.Millisecond)

		// Write result to file
		mu.Lock()
		_, err := file.WriteString(fmt.Sprintf("%s processed: %s\n", name, task))
		mu.Unlock()

		if err != nil {
			log.Printf("%s file write error: %v\n", name, err)
		} else {
			log.Printf("%s finished processing %s.\n", name, task)
		}
	}

	log.Printf("%s finished.\n", name)
}

func main() {
	numWorkers := 4
	taskCount := 10

	queue := NewTaskQueue(taskCount)
	var wg sync.WaitGroup
	var mu sync.Mutex

	file, err := os.Create("results_go.txt")
	if err != nil {
		log.Fatalf("Failed to open file: %v", err)
	}
	defer file.Close()

	for i := 1; i <= taskCount; i++ {
		queue.AddTask(fmt.Sprintf("Task-%d", i))
	}
	close(queue.tasks)

	for i := 1; i <= numWorkers; i++ {
		wg.Add(1)
		go worker(fmt.Sprintf("Worker-%d", i), queue, &wg, &mu, file)
	}

	wg.Wait()
	log.Println("All tasks completed.")
}
