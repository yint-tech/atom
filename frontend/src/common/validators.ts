const checked = (
  value: unknown,
  options: { message?: string }
): string | undefined => {
  if (value !== true) {
    return options.message || 'must be checked';
  }
};

const validators = {
  checked,
};

export default validators;
