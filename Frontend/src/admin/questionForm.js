export function updateQuestionOptions(options, selectedIndex, key, value) {
  return options.map((option, index) => {
    if (key === "correct") {
      return { ...option, correct: index === selectedIndex };
    }
    if (index !== selectedIndex) {
      return option;
    }
    return {
      ...option,
      [key]: key === "optionOrder" ? Number(value) : value,
    };
  });
}
