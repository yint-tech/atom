const checked = (value, options) => {
  if (value !== true) {
    return options.message || 'must be checked';
  }
};

const validators = {
  checked,
};

export default validators;
