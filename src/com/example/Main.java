package com.example;

import java.util.*;

public class Main {

    // Структура для хранения высказывания игрока
    static class Statement {
        int[] suspects;          // Список подозреваемых
        int[] peaceFulPlayers;   // Список мирных игроков

        public Statement(int[] suspects, int[] peaceFulPlayers) {
            this.suspects = suspects;
            this.peaceFulPlayers = peaceFulPlayers;
        }
    }

    // Метод для оценки соответствия гипотезы о мафии заявленным подозрениям
    private static boolean checkHypothesis(int[] mafiaSet, Statement statement) {
        // Оба подозреваемых должны входить в список мафии,
        // А ни один из мирных игроков не должен находиться в мафии
        return containsAll(mafiaSet, statement.suspects) && !containsAny(mafiaSet, statement.peaceFulPlayers);
    }

    // Вспомогательные методы для проверки включенности элементов в множества
    private static boolean containsAll(int[] set1, int[] set2) {
        for (int num : set2) {
            if (!contains(set1, num)) return false;
        }
        return true;
    }

    private static boolean contains(int[] array, int value) {
        for (int element : array) {
            if (element == value) return true;
        }
        return false;
    }

    private static boolean containsAny(int[] set1, int[] set2) {
        for (int num : set2) {
            if (contains(set1, num)) return true;
        }
        return false;
    }

    // Генерация всех возможных троек мафий из набора чисел
    private static List<int[]> generateCombinations(int n, int k) {
        List<int[]> combinations = new ArrayList<>();
        int[] currentCombination = new int[k];
        backtrack(combinations, currentCombination, 0, 0, n);
        return combinations;
    }

    // Рекурсивная функция для генерации комбинаций методом перебора
    private static void backtrack(List<int[]> result, int[] current, int start, int depth, int total) {
        if (depth == current.length) {
            result.add(current.clone());
            return;
        }
        for (int i = start; i <= total - (current.length - depth); ++i) {
            current[depth] = i;
            backtrack(result, current, i + 1, depth + 1, total);
        }
    }

    // Основной метод расчета наилучшей гипотезы
    public static int[] findBestMafiaSet(Statement[] statements) {
        List<int[]> allPossibleSets = generateCombinations(10, 3); // Всего три мафии из 10 игроков
        Map<String, Integer> scores = new HashMap<>(); // Результаты подсчета очков

        for (int[] mafiaSet : allPossibleSets) {
            String key = Arrays.toString(mafiaSet);
            int score = 0;

            // Оцениваем каждую гипотезу относительно заявлений игроков
            for (Statement statement : statements) {
                if (checkHypothesis(mafiaSet, statement)) {
                    score++; // За каждое удачное попадание увеличиваем счётчик
                }
            }
            scores.put(key, score);
        }

        // Найдем оптимальную гипотезу
        String bestKey = Collections.max(scores.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
        return Arrays.stream(bestKey.substring(1, bestKey.length() - 1).split(", "))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        final int NUM_PLAYERS = 10; // Общее количество игроков
        Statement[] statements = new Statement[NUM_PLAYERS]; // Массив заявлений игроков

        // Сбор данных от пользователей
        for (int i = 0; i < NUM_PLAYERS; i++) {
            System.out.print("Количество подозреваемых для игрока №" + (i + 1) + ": ");
            int numSuspects = scanner.nextInt();
            int[] suspects = new int[numSuspects];
            for (int j = 0; j < numSuspects; j++) {
                System.out.print("Подозреваешь игрока №");
                suspects[j] = scanner.nextInt() - 1;
            }

            System.out.print("Количество мирных игроков для игрока №" + (i + 1) + ": ");
            int numPeaceful = scanner.nextInt();
            int[] peaceFulPlayers = new int[numPeaceful];
            for (int j = 0; j < numPeaceful; j++) {
                System.out.print("Мирный игрок №");
                peaceFulPlayers[j] = scanner.nextInt() - 1;
            }

            statements[i] = new Statement(suspects, peaceFulPlayers);
        }

        // Выполнение основного расчёта
        int[] bestMafiaSet = findBestMafiaSet(statements);
        System.out.println("\nНаиболее вероятная группа мафии: " + Arrays.toString(bestMafiaSet));

        // Дополнительная статистика и детализация
        List<int[]> allPossibleSets = generateCombinations(10, 3); // Всего три мафии из 10 игроков
        Map<String, Integer> scores = new HashMap<>();

        for (int[] mafiaSet : allPossibleSets) {
            String key = Arrays.toString(mafiaSet);
            int score = 0;

            // Оцениваем каждую гипотезу относительно заявлений игроков
            for (Statement statement : statements) {
                if (checkHypothesis(mafiaSet, statement)) {
                    score++; // За каждое удачное попадание увеличиваем счётчик
                }
            }
            scores.put(key, score);
        }

        // Отображение дополнительной статистики
        System.out.println("\nПодробная статистика по всем гипотезам:");
        scores.forEach((key, value) -> System.out.println("Гипотеза: " + key + ", Очки: " + value));
    }
}