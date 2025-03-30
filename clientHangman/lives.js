document.addEventListener("DOMContentLoaded", ()=> {
    let counterDisplayElement = document.querySelector('.lives-display');
    let counterPlusElement = document.querySelector('.lives-plus');
    let counterMinusElement = document.querySelector('.lives-minus');
    let hangImageElement = document.querySelector('.hang-img');
    let guessInputElement = document.querySelector('.guess');
    let soundEffectElement = new Audio('gun.mp3');
    let word;

    const wordModal = document.querySelector(".word-modal");
    const wordInput = document.querySelector(".word-input");
    const applyWordBtn = document.querySelector(".apply-word");
    const stupid = document.querySelector(".stupid");

    let count = 0;
    let guessString = "";

    updateDisplay();

    guessInputElement.addEventListener("input", updateGuess);
    applyWordBtn.addEventListener("click", applyWordToServer);
    stupid.addEventListener("click", showWordModal);

    counterPlusElement.addEventListener("click", ()=>{
        count++;
        soundEffectElement.play();
        updateDisplay();
    });

    counterMinusElement.addEventListener("click", ()=>{
        count--;
        soundEffectElement.play();
        updateDisplay();
    });

    function showWordModal() {
        wordModal.style.display = "flex";
    }
    
    function hideWordModal() {
        wordModal.style.display = "none";
    }

    function applyWordToServer() {
        word = wordInput.value;
                
        if (!word) {
            alert("Please enter both server address and port");
            return;
        }
        
        hideWordModal();
    }

    function updateDisplay(){
        switch(count){
            case 0:
                hangImageElement.src = 'img/stage1.png';
                break;
            case 1:
                hangImageElement.src = 'img/stage2.png';
                break;
            case 2:
                hangImageElement.src = 'img/stage3.png';
                break;
            case 3:
                hangImageElement.src = 'img/stage4.png';
                break;
            case 4:
                hangImageElement.src = 'img/stage5.png';
                break;
            default:
                hangImageElement.src = 'img/pearto.jpg'
                break;        
        } 
    }

    function updateGuess(e){
        guessString = e.target.value;
        console.log(guessString);
    }

});