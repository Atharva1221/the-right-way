const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const phonePattern = /^(?=(?:\D*\d){7,15}\D*$)\+?[0-9](?:[0-9 ()-]*[0-9])$/;

export function validateRegistration(fields) {
  const errors = {};
  const name = fields.name.trim();
  const email = fields.email.trim();
  const phoneNumber = fields.phoneNumber.trim();

  if (!name) errors.name = "Name is required.";
  else if (name.length > 100) errors.name = "Name cannot exceed 100 characters.";
  if (!email) errors.email = "Email is required.";
  else if (email.length > 150) errors.email = "Email cannot exceed 150 characters.";
  else if (!emailPattern.test(email)) errors.email = "Enter a valid email address.";
  if (!phoneNumber) errors.phoneNumber = "Phone number is required.";
  else if (phoneNumber.length > 25) errors.phoneNumber = "Phone number cannot exceed 25 characters.";
  else if (!phonePattern.test(phoneNumber)) {
    errors.phoneNumber = "Enter a valid international phone number with 7 to 15 digits.";
  }
  if (fields.password.length < 8 || fields.password.length > 72) {
    errors.password = "Password must be between 8 and 72 characters.";
  }
  if (fields.confirmPassword !== fields.password) {
    errors.confirmPassword = "Passwords do not match.";
  }
  return errors;
}

export function validatePhoneNumber(value) {
  const phoneNumber = value.trim();
  return phoneNumber.length <= 25 && phonePattern.test(phoneNumber);
}
