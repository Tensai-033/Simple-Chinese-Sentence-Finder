import csv
import sys
from pypinyin import pinyin, Style
from colorama import Fore, init

# Import library konversi
try:
    import opencc
except ImportError:
    print("Library 'opencc' belum terinstall.")
    print("Silakan install dengan perintah: pip install opencc")
    sys.exit()

# Inisialisasi pewarnaan terminal
init(autoreset=True)

def load_dataset(file_path):
    """
    Membaca file .csv dan menyimpannya ke dalam list dictionary.
    """
    dataset = []
    try:
        with open(file_path, mode='r', encoding='utf-8') as csvfile:
            reader = csv.reader(csvfile)
            print(f"{Fore.CYAN}Sedang memuat dataset dari {file_path}...")
            
            for row in reader:
                if len(row) >= 4:
                    item = {
                        "hanzi": row[1],
                        "english": row[3]
                    }
                    dataset.append(item)
                    
        print(f"{Fore.GREEN}[SUCCESS] Berhasil memuat {len(dataset)} kalimat mentah.")
        return dataset

    except FileNotFoundError:
        print(f"{Fore.RED}[ERROR] File {file_path} tidak ditemukan!")
        return []
    except Exception as e:
        print(f"{Fore.RED}[ERROR] Terjadi kesalahan: {e}")
        return []

def convert_dataset_and_get_converter(dataset, mode):
    """
    Mengonversi dataset DAN mengembalikan object converter-nya
    agar bisa dipakai untuk mengonversi input user nanti.
    """
    converter = opencc.OpenCC(mode)
    print(f"{Fore.YELLOW}Sedang mengonversi dataset ke mode {'Traditional' if mode == 's2t' else 'Simplified'}...")
    
    for item in dataset:
        # Konversi Hanzi di dataset
        item['hanzi'] = converter.convert(item['hanzi'])
        
    print(f"{Fore.GREEN}[INFO] Konversi selesai! Siap digunakan.")
    return converter

def generate_pinyin_sentence(hanzi_text):
    """Mengubah kalimat Hanzi menjadi string Pinyin"""
    pinyin_list = pinyin(hanzi_text, style=Style.TONE)
    pinyin_str = " ".join([item[0] for item in pinyin_list])
    return pinyin_str

def search_vocab(keyword, dataset):
    """Mencari keyword dalam dataset"""
    results = []
    
    for item in dataset:
        if keyword in item['hanzi']:
            pinyin_text = generate_pinyin_sentence(item['hanzi'])
            result = {
                "hanzi": item['hanzi'],
                "pinyin": pinyin_text,
                "english": item['english']
            }
            results.append(result)
            if len(results) >= 10:
                break
    return results

def main():
    nama_file_csv = "sentence.csv" 
    
    # 1. Load Data Mentah
    data_sentences = load_dataset(nama_file_csv)
    if not data_sentences:
        return

    # 2. Pilihan Mode
    print("-" * 50)
    print("PILIH MODE AKSARA:")
    print("1. Simplified Chinese (简体) - Contoh: 学, 爱")
    print("2. Traditional Chinese (繁体) - Contoh: 學, 愛")
    
    active_converter = None # Variabel untuk menyimpan converter yang dipilih
    
    while True:
        choice = input("Masukkan pilihan (1/2): ").strip()
        if choice == '1':
            # Simpan converter yang dipakai
            active_converter = convert_dataset_and_get_converter(data_sentences, 't2s') 
            break
        elif choice == '2':
            # Simpan converter yang dipakai
            active_converter = convert_dataset_and_get_converter(data_sentences, 's2t')
            break
        else:
            print(f"{Fore.RED}Pilihan tidak valid. Ketik 1 atau 2.")

    # 3. Loop Pencarian
    while True:
        print("-" * 50)
        keyword_raw = input("Masukkan Kosakata (atau 'q' untuk keluar): ").strip()
        
        if keyword_raw.lower() == 'q':
            print("Zaijian!")
            break
            
        if not keyword_raw:
            continue

        # --- PERBAIKAN UTAMA DISINI ---
        # Input user dikonversi juga menggunakan converter yang sama dengan dataset
        keyword_search = active_converter.convert(keyword_raw)
        
        # Tampilkan feedback ke user apa yang sebenarnya dicari oleh sistem
        print(f"\n{Fore.CYAN}Mencari kata: '{keyword_search}' (Input asli: {keyword_raw})...")

        hasil_pencarian = search_vocab(keyword_search, data_sentences)

        if hasil_pencarian:
            print(f"Ditemukan {len(hasil_pencarian)} contoh kalimat:\n")
            for i, item in enumerate(hasil_pencarian, 1):
                print(f"{Fore.YELLOW}Contoh #{i}")
                print(f"Hanzi  : {Fore.WHITE}{item['hanzi']}")
                print(f"Pinyin : {Fore.LIGHTBLACK_EX}{item['pinyin']}")
                print(f"Inggris: {Fore.GREEN}{item['english']}")
                print("")
        else:
            print(f"{Fore.RED}Kata '{keyword_search}' tidak ditemukan.")

if __name__ == "__main__":
    main()