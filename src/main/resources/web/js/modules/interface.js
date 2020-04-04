const userToggle = $('#user-toggle');
const adminToggle = $('#admin-toggle');
const userPanel = $('#user');
const adminPanel = $('#admin');

userToggle.click(() => {
    adminPanel.hide();
    userPanel.show();
});

adminToggle.click(() => {
    userPanel.hide();
    adminPanel.show();
});

adminPanel.hide();

const refreshButton = $("#refresh");

function prependWithZero(n) {
    return n < 10 ? "0" + n : n;
}

