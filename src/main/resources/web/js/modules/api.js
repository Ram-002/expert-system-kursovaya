const Stock = Object.seal({
    selectedStock: null,
    get: () => new Promise((resolve, reject) =>
        $.get(
            url + "stock"
        ).done(value => {
            resolve(value);
        }).fail(value => {
            reject(value);
        })),
    add: name => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "stock",
            type: "PUT",
            contentType: 'application/json',
            data: JSON.stringify({name: String(name)})
        }).done(() => {
            resolve(null);
        }).fail(() => {
            reject(null);
        })),
    delete: name => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "stock",
            type: "DELETE",
            contentType: 'application/json',
            data: JSON.stringify({name: String(name)})
        }).done(() => {
            resolve(null);
        }).fail(() => {
            reject(null);
        })),
    getPrice: () => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "price",
            type: "POST",
            contentType: 'application/json',
            data: JSON.stringify({name: String(Stock.selectedStock)}),
            processData: false
        }).done((prices) => {
            resolve(prices);
        }).fail(() => {
            reject(null);
        })),
    addPrice: (date, price, dividends) => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "price",
            type: "PUT",
            contentType: 'application/json',
            data: JSON.stringify({
                name: String(Stock.selectedStock),
                date: date,
                price: price,
                dividends: Number(dividends)
            })
        }).done(() => {
            resolve();
        }).fail(() => {
            reject();
        }))
});

const Portfolio = Object.seal({
    username: null,
    get: () => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "portfolio",
            type: "POST",
            contentType: 'application/json',
            data: JSON.stringify({user: String(Portfolio.username)}),
            processData: false
        }).done(value => {
            resolve(value);
        }).fail(() => {
            reject(null);
        })),
    add: (day, stock, amount) => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "portfolio",
            type: "PUT",
            contentType: 'application/json',
            data: JSON.stringify({
                user: String(Portfolio.username),
                name: String(stock),
                date: Number(day),
                amount: Number(amount)
            }),
            processData: false
        }).done(() => {
            resolve(null);
        }).fail(() => {
            reject(null);
        })),
    remove: (day, stock) => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "portfolio",
            type: "DELETE",
            contentType: 'application/json',
            data: JSON.stringify({
                user: String(Portfolio.username),
                name: String(stock),
                date: Number(day)
            }),
            processData: false
        }).done(() => {
            resolve(null);
        }).fail(() => {
            reject(null);
        })),
    analytics: () => new Promise((resolve, reject) =>
        $.ajax({
            url: url + "analytics",
            type: "POST",
            contentType: 'application/json',
            data: JSON.stringify({user: String(Portfolio.username)}),
            processData: false
        }).done(value => {
            resolve(value);
        }).fail(() => {
            reject(null);
        })),
});
