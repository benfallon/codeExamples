import UIKit
import CircularCarousel
import FirebaseStorage
import PullToDismiss
import FirebaseDatabase
import CoreLocation

protocol PhotoCellTappedDelegate {
    func photoCellTapped(photoUUID : String)
}

final class DestinationDetailsVC: UIViewController, UITableViewDataSource, UITableViewDelegate,
    ButtonCarouselViewDelegate,ButtonCarouselViewDataSource, TableCarouselViewDelegate, PhotoCellTappedDelegate, GPSButtonTappedDelegate {
    
    /** Outlets **/
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var imageView: CustomImageView!
    @IBOutlet weak var gradientView: UIView!
    @IBOutlet weak var whiteBottomView: UIView!
    
    @IBOutlet weak var destType: UILabel!
    @IBOutlet weak var destName: UILabel!
    @IBOutlet weak var destStateCountry: UILabel!
    
    @IBOutlet weak var categoryName: UILabel!
    
    @IBOutlet weak var noPhotosTextLabel: UILabel!
    
    /** Constants **/
    let categoryNames = ["all", "backpacking",  "citySightseeing", "culinary", "family", "history", "outdoor", "roadTrip", "campingAndRV", "smallTown"]
    
    /** Views / UI **/
    private var tableCarouselView: TableCarouselView?
    private var buttonCarouselView: ButtonCarouselView?
    private var categoryNameView : UILabel = UILabel()
    private var selectedItemIndex = ViewConstants.startingCarouselItem
    
    /** Firebase Storage (user photos) and Real-Time Database (destination data) **/
    private var storage: Storage?
    private var ref: DatabaseReference?
    
    //** Destination **//
    var selectedDestination: Destination?
    var allDestPhotos : [UntraveledPhoto]?
    private var destPhotosByCategory : [String : [UntraveledPhoto]]?
    
    //** User Interaction **//
    let interactor = Interactor()
    private var pullToDismiss: PullToDismiss?
    
    var gpsButtonTappedDelegate : GPSButtonTappedDelegate?
    
     /** Override **/
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return .portrait
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        gradientView.layer.sublayers?[0].frame = gradientView.layer.bounds
        applyImageScale(withScrollView: tableView)
    }
    
    /** Protocol Implementations **/
    func goToMapAndCenterAt(location: CLLocation) {
        dismiss(animated: true, completion: {
            self.gpsButtonTappedDelegate!.goToMapAndCenterAt(location: location)
        })
    }
    
    func photoCellTapped(photoUUID: String) {
        launchFullScreenPhotoVC(photoId: photoUUID)
    }
    
    //** Initialization **//
    override func viewDidLoad() {
        super.viewDidLoad()
        storage = Storage.storage()
        ref = Database.database().reference()
        
        pullToDismiss = PullToDismiss(scrollView: tableView)
        pullToDismiss?.delegate = self
        
        styleViews()
        configureViews()
        fetchAndSetPrimaryImageForDestDetailsVC()
        
        destType.text = selectedDestination?.type
        destName.text = selectedDestination?.name
        destStateCountry.text = "\(selectedDestination!.stateRegionName!) | \(selectedDestination!.countryDisplayName!)"
        
        destPhotosByCategory = sortByCategory(destPhotos: self.allDestPhotos!)
    }
    
    //** Custom Untraveled Implementations **//
    func launchFullScreenPhotoVC(photoId : String) {
        let selectedUntraveledPhoto = getUntraveledPhotoById(id: photoId)
        
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        
        let photoDetailsViewController =  storyboard.instantiateViewController(withIdentifier: "PhotoDetailsViewController") as! PhotoDetailsViewController
        
        photoDetailsViewController.selectedPhoto = selectedUntraveledPhoto
        
        photoDetailsViewController.gpsButtonTappedDelegate = self as! GPSButtonTappedDelegate
        
        photoDetailsViewController.transitioningDelegate = self
        photoDetailsViewController.interactor = interactor
        
        self.present(photoDetailsViewController, animated: true, completion: nil)
    }
    
    func getUntraveledPhotoById(id: String) -> UntraveledPhoto? {
        for photo in allDestPhotos! {
            if photo.photoUUID == id {
                return photo
            }
        }
        
        return nil
    }
    
    func fetchAndSetPrimaryImageForDestDetailsVC() {
        let primaryImageId = selectedDestination!.primaryPhotoId
        
        let suffix : String = selectedDestination!.fileNameSuffix ?? "default"
        let storageImageFileName = String(primaryImageId) + "_" + suffix + "-primary.jpg"
        
        let primaryImagetoShowRef = self.storage!.reference().child("flamelink/media/" + storageImageFileName)
        
        // Fetch the download URL
        primaryImagetoShowRef.downloadURL { url, error in
            if error == nil {
                self.imageView.loadImageUsingUrlString(urlString: (url?.absoluteString)!)
            } else {
                self.imageView.image = UIImage(named: "sample_bg_0")
            }
        }
    }
    
    private func applyImageScale(withScrollView scrollView: UIScrollView) {
        whiteBottomView.frame = CGRect(origin: CGPoint(x: 0,
                                                       y: scrollView.contentSize.height - scrollView.contentOffset.y + 0),
                                       size: whiteBottomView.frame.size)
        let minScale:CGFloat = 1.1
        let maxScale:CGFloat = 2.0
        
        let offset = tableView.contentOffset.y
        let height = tableView.contentSize.height
        
        var scale = (1.0 / height) * offset
        
        scale = scale * (maxScale - minScale)
        scale += minScale
        
        imageView.applyScale(scale)
    }
    
    func addCategoryLabel(cell : UITableViewCell) {
        categoryNameView = UILabel(frame: CGRect(x: 0, y: 0, width: cell.layer.bounds.width, height: cell.layer.bounds.height))
        categoryNameView.text = ""
        categoryNameView.translatesAutoresizingMaskIntoConstraints = false
        categoryNameView.textColor = UIColor(red:0.42, green:0.53, blue:1.00, alpha:1.0)
        categoryNameView.textAlignment = .center
        categoryNameView.font = UIFont(name: "Poppins-Regular", size: 24)
        categoryNameView.contentMode = UIView.ContentMode.scaleAspectFit
        
        cell.addSubview(categoryNameView)
        
        categoryNameView.centerXAnchor.constraint(equalTo: cell.centerXAnchor).isActive = true
        categoryNameView.bottomAnchor.constraint(equalTo: cell.topAnchor, constant: 75).isActive = true
    }
    
    func sortByCategory(destPhotos : [UntraveledPhoto]) -> [String : [UntraveledPhoto]]{
        var destPhotosByCategory : [String : [UntraveledPhoto]] = [:]
        destPhotosByCategory["all"] = []
        for photo in destPhotos {
            // add every photo to the "all" list
            destPhotosByCategory["all"]!.append(photo)

            // add photo to every list for which it is a relevant category
            for category in photo.relevantActivityCategories ?? [] {
                let categoryVarName = getActivityCategoryVariableNameFromDisplayName(displayName: category.activityCategoryName)
                
                if destPhotosByCategory[categoryVarName] == nil {
                    // make sure array exists so we can append to it
                    destPhotosByCategory[categoryVarName] = []
                }
                destPhotosByCategory[categoryVarName]!.append(photo)
            }
        }
        return destPhotosByCategory
    }
    
    func getActivityCategoryVariableNameFromDisplayName(displayName : String) -> String {
        var mapping:[String:String] = ["Outdoor":"outdoor", "Road Trip":"roadTrip", "City Sightseeing":"citySightseeing", "History": "history", "smallTowns":"smallTown", "Backpacking":"backpacking", "Camping & RV":"campingAndRV", "Family":"family", "Culinary":"culinary"]
        
        return mapping[displayName] ?? displayName
    }
    
    //** UITableView **//
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let row = indexPath.row
        
        switch row {
        case 0:
            let cell = UITableViewCell()
            cell.backgroundColor = UIColor.clear
            cell.separatorInset = UIEdgeInsets(top: 0.0, left: tableView.bounds.size.width, bottom: 0.0, right: 0.0)
            
            return cell
            
        case ViewConstants.RowIndex.tableCarousel:
            if let tableCarouselView = self.tableCarouselView {
                // Cache the table Carousel view - There is only one
                return tableCarouselView
            } else {
                let cell: TableCarouselView = tableView.dequeueReusableCell(withIdentifier: ViewConstants.CellIdentifiers.tableCarousel) as! TableCarouselView
                
                cell.delegate = self
                
                cell.photoCellTappedDelegate = self
                
                cell.carousel.panEnabled = false
                cell.carousel.swipeEnabled = false
                cell.carousel.reloadData()
                tableCarouselView = cell
                tableCarouselView!.allDestPhotosByCategory = destPhotosByCategory
                
                return cell
            }
            
        case ViewConstants.RowIndex.buttonCarousel:
            let cell: ButtonCarouselView = tableView.dequeueReusableCell(withIdentifier: ViewConstants.CellIdentifiers.buttons) as! ButtonCarouselView
            cell.backgroundColor = UIColor.clear
            
            cell.delegate = self
            cell.dataSource = self
            
            cell.carousel.panEnabled = false
            cell.carousel.swipeEnabled = true
            
            // Only add it once, otherwise just update the text
            if (!categoryNameView.isDescendant(of: cell)) {
                addCategoryLabel(cell: cell)
            }
            
            cell.carousel.reloadData()
            
            buttonCarouselView = cell
            
            return cell
            
        default:
            return UITableViewCell()
        }
    }
    
    private func styleViews() {
        tableView.style(withDetail: .primary)
    }
    
    private func configureViews() {
        configureTableView()
    }
    
    private func configureTableView() {
        tableView.register(UINib(nibName: ViewConstants.NibNames.tableCarousel, bundle: nil), forCellReuseIdentifier: ViewConstants.CellIdentifiers.tableCarousel)
        tableView.register(UINib(nibName: ViewConstants.NibNames.buttons, bundle: nil), forCellReuseIdentifier: ViewConstants.CellIdentifiers.buttons)
        tableView.separatorInset.left = 0
        tableView.bounds = CGRect(x: 0,
                                  y: 0,
                                  width: tableView.bounds.size.width,
                                  height: tableView.bounds.size.height * 2)
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return ViewConstants.numberOfPrimaryViewRows
    }
    
    func tableView(_ tableView: UITableView,
                   willDisplay cell: UITableViewCell,
                   forRowAt indexPath: IndexPath) {
        if indexPath.row == 0 {
            cell.backgroundView = nil
            cell.backgroundColor = UIColor.clear
            cell.layer.backgroundColor = UIColor.clear.cgColor
        }
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        switch indexPath.row {
        case 0:
            let height = view.bounds.height
            let margin = ViewConstants.topRowScreenRatio * height
            return margin
        case ViewConstants.RowIndex.buttonCarousel:
            return ViewConstants.CellHeights.buttonsCarousel
        case ViewConstants.RowIndex.tableCarousel:
            return ViewConstants.CellHeights.image * CGFloat(allDestPhotos!.count)
        default:
            return ViewConstants.CellHeights.normal
        }
    }

    //** ButtonCarousel **//
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        applyImageScale(withScrollView: scrollView)
    }
    
    func buttonCarousel(_ buttonCarousel: ButtonCarouselView, modelForIndex index: IndexPath) -> ButtonCarouselModel {
        return CarouselData.buttonViewModels[index.row]
    }
    
    func numberOfButtons(inButtonCarousel buttonCarousel: ButtonCarouselView) -> Int {
        return CarouselData.buttonViewModels.count
    }
    
    func buttonCarousel(_ carousel: ButtonCarouselView, buttonPressed button: UIButton) {}
    
    func buttonCarousel(_ carousel: ButtonCarouselView, willScrollToIndex index: IndexPath) {
        selectedItemIndex = index.row
        
        let viewModel = CarouselData.buttonViewModels[selectedItemIndex]
        categoryNameView.text = viewModel.text
        
        tableCarouselView?.carousel.scroll(toItemAtIndex: index.row, animated: true)
    }
    
    func startingIndex(forButtonCarousel carousel: ButtonCarouselView) -> Int {
        return selectedItemIndex
    }
    
    func itemWidth(forButtonCarousel carousel: ButtonCarouselView) -> CGFloat {
        return ViewConstants.Size.carouselButtonItemWidith
    }

    func numberOfItems(inTableCarousel view: TableCarouselView) -> Int {
        return 10
    }
}

extension DestinationDetailsVC: UIViewControllerTransitioningDelegate {
    func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        return DismissAnimator()
    }
    
    func interactionControllerForDismissal(using animator: UIViewControllerAnimatedTransitioning) -> UIViewControllerInteractiveTransitioning? {
        return interactor.hasStarted ? interactor : nil
    }
}
