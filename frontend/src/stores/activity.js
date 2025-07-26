import { createPinia, defineStore } from "pinia";

function hasDuplicateObject(arr, newObj) {
    // 判斷陣列中是否有相同屬性值的物件
    return arr.some(existingObj =>
        Object.keys(existingObj).every(key => existingObj[key] === newObj[key])
    );
}

export const useActivityStore = defineStore('activity', {
    state: () => ({
        act: [],
    }),

    actions: {
        addHotel(hotel, type, tripId) {
            console.log("test");
            const newAct = {
                tripId: tripId,
                orderType: type,
                ...hotel,
            }
            if (!hasDuplicateObject(this.act, newAct)) {
                this.act.push(newAct);
            }
        },

        addFlight(flight, type, tripId) {
            const newAct = {
                tripId: tripId,
                orderType: type,
                ...flight,
            };
            if (!hasDuplicateObject(this.act, newAct)) {
                this.act.push(newAct);
            }
        },

        addAttract(ticket, type, tripId) {
            const newAct = {
                tripId: tripId,
                orderType: type,
                ...ticket,
            };
            if (!hasDuplicateObject(this.act, newAct)) {
                this.act.push(newAct);
            }
        },

        addSpot(spot, type, tripId) {
            const newAct = {
                tripId: tripId,
                orderType: type,
                ...spot,
            };
            if (!hasDuplicateObject(this.act, newAct)) {
                this.act.push(newAct);
            }
        },

        clean(type, tripId) {
            let index;
            while ((index = this.act.findIndex(a => a.orderType === type && a.tripId === tripId)) !== -1) {
                this.act.splice(index, 1);
            }
        },
    },
    persist: true,
})